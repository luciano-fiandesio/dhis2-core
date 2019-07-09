package org.hisp.dhis.setting;

/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.system.util.ValidationUtils;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.env.Environment;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Declare transactions on individual methods. The get-methods do not have
 * transactions declared, instead a programmatic transaction is initiated on
 * cache miss in order to reduce the number of transactions to improve performance.
 *
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
public class DefaultSystemSettingManager
    implements SystemSettingManager
{
    private static final Map<String, SettingKey> NAME_KEY_MAP = Lists.newArrayList(
            SettingKey.values() ).stream().collect( Collectors.toMap( SettingKey::getName, e -> e ) );

    /**
     * Cache for system settings. Does not accept nulls. Disabled during test phase.
     */
    private Cache<Serializable> settingCache;
    private LoadingCache<SettingKey, Serializable> loadingCache;
    private AsyncLoadingCache<SettingKey, Serializable> asyncloadingCache;
    private org.cache2k.Cache<SettingKey, Serializable> cache2K;


    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private static final Log log = LogFactory.getLog( DefaultSystemSettingManager.class );

    private SystemSettingStore systemSettingStore;

    private TransactionTemplate transactionTemplate;

    private PBEStringEncryptor pbeStringEncryptor;

    private CacheProvider cacheProvider;

    private Environment environment;

    private List<String> flags;

    public DefaultSystemSettingManager( SystemSettingStore systemSettingStore, TransactionTemplate transactionTemplate,
        @Qualifier( "tripleDesStringEncryptor" ) PBEStringEncryptor pbeStringEncryptor, CacheProvider cacheProvider,
        Environment environment, List<String> flags )
    {
        checkNotNull( systemSettingStore );
        checkNotNull( transactionTemplate );
        checkNotNull( pbeStringEncryptor );
        checkNotNull( cacheProvider );
        checkNotNull( environment );
        checkNotNull( flags );

        this.systemSettingStore = systemSettingStore;
        this.transactionTemplate = transactionTemplate;
        this.pbeStringEncryptor = pbeStringEncryptor;
        this.cacheProvider = cacheProvider;
        this.environment = environment;
        this.flags = flags;
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    @PostConstruct
    public void init()
    {
        settingCache = cacheProvider.newCacheBuilder( Serializable.class ).forRegion( "systemSetting" )
            .expireAfterWrite( 3, TimeUnit.SECONDS )
            .withMaximumSize( 400 ).build();

//       loadingCache = Caffeine.newBuilder()
//                .expireAfterWrite(3, TimeUnit.SECONDS)
//                .maximumSize( 10000 )
//                .recordStats()
//                .build(k -> getRandomVal( k.getName(), k.getDefaultValue()  ).orElse(null));
//
//
//        asyncloadingCache = Caffeine.newBuilder()
//                .expireAfterWrite(500, TimeUnit.MILLISECONDS)
//                .maximumSize( 400 )
//                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> getRandomVal( key.getName(), key.getDefaultValue()  ).orElse(null)));

        cache2K = new Cache2kBuilder<SettingKey, Serializable>() {}
                .expireAfterWrite(5, TimeUnit.MINUTES)    // expire/refresh after 5 minutes
                .resilienceDuration(30, TimeUnit.SECONDS) // cope with at most 30 seconds
                // outage before propagating
                // exceptions
                .refreshAhead(true)                       // keep fresh when expiring
                .loader( k -> getRandomVal( k.getName(), k.getDefaultValue()  ).orElse(null))         // auto populating function
                .build();

    }

    // -------------------------------------------------------------------------
    // SystemSettingManager implementation
    // -------------------------------------------------------------------------

    private Optional<Serializable> getRandomVal( String name, Serializable defaultValue ) {

        try {
            Thread.sleep((long)(Math.random() * 1500));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Optional.of(RandomStringUtils.randomAlphabetic(10));
    }

    @Override
    @Transactional
    public void saveSystemSetting( SettingKey key, Serializable value )
    {
        settingCache.invalidate( key.getName() );

        SystemSetting setting = systemSettingStore.getByName( key.getName() );

        if ( isConfidential( key.getName() ) )
        {
            value = pbeStringEncryptor.encrypt( value.toString() );
        }

        if ( setting == null )
        {
            setting = new SystemSetting();

            setting.setName( key.getName() );
            setting.setValue( value );

            systemSettingStore.save( setting );
        }
        else
        {
            setting.setValue( value );

            systemSettingStore.update( setting );
        }
    }

    @Override
    @Transactional
    public void deleteSystemSetting( SettingKey key )
    {
        SystemSetting setting = systemSettingStore.getByName( key.getName() );

        if ( setting != null )
        {
            settingCache.invalidate( key.getName() );

            systemSettingStore.delete( setting );
        }
    }

    /**
     * No transaction for this method, transaction is initiated in
     * {@link #getSystemSettingOptional} on cache miss.
     */
    public Serializable getSystemSettingAsync( SettingKey key )
    {
        //System.out.println(">> " + key.getName());
//        Optional<Serializable> value = settingCache.get( key.getName(),
//            k -> getSystemSettingOptional( k, key.getDefaultValue() ).orElse( null ) );
        //System.out.println(loadingCache.stats().toString());
        long missCount = asyncloadingCache.synchronous().stats().missCount();
        if (missCount % 10 == 0 && missCount != 0) {
            System.out.println("missCount" + missCount + ", hitCount: " + asyncloadingCache.synchronous().stats().hitCount());
        }

        Optional<Serializable> value = null;
        try {
            value = Optional.ofNullable(asyncloadingCache.get(key).get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return value.orElse( null );
    }
    @Override
    public Serializable getSystemSetting( SettingKey key )
    {
        Optional<Serializable> value = Optional.ofNullable(cache2K.get(key));
        return value.orElse( null );
    }

    public Serializable getSystemSettingOrig( SettingKey key )
    {
        Optional<Serializable> value = settingCache.get( key.getName(),
                k -> getRandomVal( k, key.getDefaultValue() ).orElse( null ) );

        return value.orElse( null );
    }

    /**
     * No transaction for this method, transaction is initiated in
     * {@link #getSystemSettingOptional}.
     */
    @Override
    public Serializable getSystemSetting( SettingKey key, Serializable defaultValue )
    {
        return getSystemSettingOptional( key.getName(), defaultValue ).orElse( null );
    }

    /**
     * Get system setting optional. The database call is executed in a
     * programmatic transaction.
     *
     * @param name the system setting name.
     * @param defaultValue the default value for the system setting.
     * @return an optional system setting value.
     */
    private Optional<Serializable> getSystemSettingOptional( String name, Serializable defaultValue )
    {
        SystemSetting setting = transactionTemplate.execute(status -> systemSettingStore.getByName( name ));
//        SystemSetting setting = systemSettingStore.getByName( name );
        if ( setting != null && setting.hasValue() )
        {
            if ( isConfidential( name ) )
            {
                try
                {
                    return Optional.of( pbeStringEncryptor.decrypt( (String) setting.getValue() ) );
                }
                catch ( EncryptionOperationNotPossibleException e ) // Most likely this means the value is not encrypted, or not existing
                {
                    log.warn( "Could not decrypt system setting '" + name + "'" );
                    return Optional.empty();
                }
            }
            else
            {
                return Optional.of( setting.getValue() );
            }
        }
        else
        {
            return Optional.ofNullable( defaultValue );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSystemSettings()
    {
        return systemSettingStore.getAll().stream().
            filter( systemSetting -> !isConfidential( systemSetting.getName() ) ).
            collect( Collectors.toList() );
    }

    @Override
    public Map<String, Serializable> getSystemSettingsAsMap()
    {
        final Map<String, Serializable> settingsMap = new HashMap<>();

        for ( SettingKey key : SettingKey.values() )
        {
            if ( key.hasDefaultValue() )
            {
                settingsMap.put( key.getName(), key.getDefaultValue() );
            }
        }

        Collection<SystemSetting> systemSettings = getAllSystemSettings();

        for ( SystemSetting systemSetting : systemSettings )
        {
            Serializable settingValue = systemSetting.getValue();

            if ( settingValue == null )
            {
                Optional<SettingKey> setting = SettingKey.getByName( systemSetting.getName() );

                if ( setting.isPresent() )
                {
                    settingValue = setting.get().getDefaultValue();
                }
            }

            settingsMap.put( systemSetting.getName(), settingValue );
        }

        return settingsMap;
    }

    @Override
    public Map<String, Serializable> getSystemSettings( Collection<SettingKey> keys )
    {
        Map<String, Serializable> map = new HashMap<>();

        for ( SettingKey setting : keys )
        {
            Serializable value = getSystemSetting( setting );

            if ( value != null )
            {
                map.put( setting.getName(), value );
            }
        }

        return map;
    }

    @Override
    public void invalidateCache()
    {
        settingCache.invalidateAll();
    }

    // -------------------------------------------------------------------------
    // Specific methods
    // -------------------------------------------------------------------------

    @Override
    public List<String> getFlags()
    {
        Collections.sort( flags );
        return flags;
    }

    @Override
    public String getFlagImage()
    {
        String flag = (String) getSystemSetting( SettingKey.FLAG );

        return flag != null ? flag + ".png" : null;
    }

    @Override
    public String getEmailHostName()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.EMAIL_HOST_NAME ) );
    }

    @Override
    public int getEmailPort()
    {
        return (Integer) getSystemSetting( SettingKey.EMAIL_PORT );
    }

    @Override
    public String getEmailUsername()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.EMAIL_USERNAME ) );
    }

    @Override
    public boolean getEmailTls()
    {
        return (Boolean) getSystemSetting( SettingKey.EMAIL_TLS );
    }

    @Override
    public String getEmailSender()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.EMAIL_SENDER ) );
    }

    @Override
    public boolean accountRecoveryEnabled()
    {
        return (Boolean) getSystemSetting( SettingKey.ACCOUNT_RECOVERY );
    }

    @Override
    public boolean selfRegistrationNoRecaptcha()
    {
        return (Boolean) getSystemSetting( SettingKey.SELF_REGISTRATION_NO_RECAPTCHA );
    }

    @Override
    public boolean emailConfigured()
    {
        return StringUtils.isNotBlank( getEmailHostName() )
            && StringUtils.isNotBlank( getEmailUsername() );
    }

    @Override
    public boolean systemNotificationEmailValid()
    {
        String address = (String) getSystemSetting( SettingKey.SYSTEM_NOTIFICATIONS_EMAIL );

        return address != null && ValidationUtils.emailIsValid( address );
    }

    @Override
    public boolean hideUnapprovedDataInAnalytics()
    {
        // -1 means approval is disabled
        return (int) getSystemSetting( SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD ) >= 0;
    }

    @Override
    public boolean isOpenIdConfigured()
    {
        return getSystemSetting( SettingKey.OPENID_PROVIDER ) != null &&
            getSystemSetting( SettingKey.OPENID_PROVIDER_LABEL ) != null;
    }

    @Override
    public String googleAnalyticsUA()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.GOOGLE_ANALYTICS_UA ) );
    }

    @Override
    public Integer credentialsExpires()
    {
        return (Integer) getSystemSetting( SettingKey.CREDENTIALS_EXPIRES );
    }

    @Override
    public boolean isConfidential( String name )
    {
        return NAME_KEY_MAP.containsKey( name ) && NAME_KEY_MAP.get( name ).isConfidential();
    }
}
