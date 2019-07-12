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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hisp.dhis.DhisSpringTest;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.setting.SettingKey.*;
import static org.junit.Assert.*;

/**
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
public class SystemSettingManagerTest
    extends
    DhisSpringTest
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    @Qualifier( "tripleDesStringEncryptor" )
    private PBEStringEncryptor pbeStringEncryptor;

    @Autowired
    @Qualifier("readOnlyJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    public void setUpTest()
    {
        systemSettingManager.invalidateCache();
    }

    @Test
    public void testSaveGetSetting()
    {
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_NOTIFICATION, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( APPLICATION_NOTIFICATION ) );
    }

    @Test
    public void testSaveGetSettingWithDefault()
    {
        assertEquals( APP_STORE_URL.getDefaultValue(), systemSettingManager.getSystemSetting( APP_STORE_URL ) );
        assertEquals( EMAIL_PORT.getDefaultValue(), systemSettingManager.getSystemSetting( EMAIL_PORT ) );
    }

    @Test
    public void testSaveGetDeleteSetting()
    {
        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( HELP_PAGE_LINK, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.deleteSystemSetting( APPLICATION_INTRO );

        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.deleteSystemSetting( HELP_PAGE_LINK );

        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );
    }

    @Test
    public void testGetAllSystemSettings()
    {
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_NOTIFICATION, "valueB" );

        List<SystemSetting> settings = systemSettingManager.getAllSystemSettings();

        assertNotNull( settings );
        assertEquals( 2, settings.size() );
    }

    @Test
    public void testGetSystemSettingsAsMap()
    {
        systemSettingManager.saveSystemSetting( SettingKey.APP_STORE_URL, "valueA" );
        systemSettingManager.saveSystemSetting( SettingKey.APPLICATION_TITLE, "valueB" );
        systemSettingManager.saveSystemSetting( SettingKey.APPLICATION_NOTIFICATION, "valueC" );

        Map<String, Serializable> settingsMap = systemSettingManager.getSystemSettingsAsMap();

        assertTrue( settingsMap.containsKey( SettingKey.APP_STORE_URL.getName() ) );
        assertTrue( settingsMap.containsKey( SettingKey.APPLICATION_TITLE.getName() ) );
        assertTrue( settingsMap.containsKey( SettingKey.APPLICATION_NOTIFICATION.getName() ) );

        assertEquals( "valueA", settingsMap.get( SettingKey.APP_STORE_URL.getName() ) );
        assertEquals( "valueB", settingsMap.get( SettingKey.APPLICATION_TITLE.getName() ) );
        assertEquals( "valueC", settingsMap.get( SettingKey.APPLICATION_NOTIFICATION.getName() ) );
        assertEquals( SettingKey.CACHE_STRATEGY.getDefaultValue(),
            settingsMap.get( SettingKey.CACHE_STRATEGY.getName() ) );
        assertEquals( SettingKey.CREDENTIALS_EXPIRES.getDefaultValue(),
            settingsMap.get( SettingKey.CREDENTIALS_EXPIRES.getName() ) );
    }

    @Test
    public void testGetSystemSettingsByCollection()
    {
        Collection<SettingKey> keys = ImmutableSet.of( SettingKey.APP_STORE_URL, SettingKey.APPLICATION_TITLE,
            SettingKey.APPLICATION_INTRO );

        systemSettingManager.saveSystemSetting( APP_STORE_URL, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_TITLE, "valueB" );
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueC" );

        assertEquals( systemSettingManager.getSystemSettings( keys ).size(), 3 );

    }

    @Test
    public void testIsConfidential()
    {
        assertTrue( EMAIL_PASSWORD.isConfidential() );
        assertTrue( systemSettingManager.isConfidential( EMAIL_PASSWORD.getName() ) );

        assertFalse( EMAIL_HOST_NAME.isConfidential() );
        assertFalse( systemSettingManager.isConfidential( EMAIL_HOST_NAME.getName() ) );
    }

    @Test
    public void testLock()
        throws ExecutionException,
        InterruptedException
    {
        int threads = 100;

        ExecutorService service = Executors.newWorkStealingPool(50 );

        Collection<Future<Serializable>> futures = new ArrayList<>( threads );

        List<SettingKey> keys = new ArrayList<>();
        keys.add( APP_STORE_URL);
        keys.add( APPLICATION_TITLE);
        keys.add( APPLICATION_NOTIFICATION);

        systemSettingManager.saveSystemSetting( keys.get(0), "valueA" );
        systemSettingManager.saveSystemSetting( keys.get(1), "valueB" );
        systemSettingManager.saveSystemSetting( keys.get(2), "valueC" );

        for ( int t = 0; t < threads; ++t )
        {
//            futures.add( service.submit( () -> systemSettingManager
//                .getSystemSetting( keys.get( ThreadLocalRandom.current().nextInt( 0, 1 + 1 ) ) ) ) );

            futures.add( service.submit( () -> systemSettingManager
                .getSystemSetting( keys.get( ThreadLocalRandom.current().nextInt( 0, 1 + 1 ) ) ) ) );

        }

        List<Serializable> vals = new ArrayList<>();

        for ( Future<Serializable> f : futures )
        {
            vals.add( f.get() );
            System.out.println(f.get());
        }
        assertThat( vals.size(), equalTo( threads ) );
    }

    @Test
    public void testLock2()
            throws ExecutionException,
            InterruptedException, IOException {

//        sessionFactory.getCurrentSession().createNativeQuery(
//                "INSERT INTO systemsetting (systemsettingid, name, value) VALUES " +
//                        "(10001, 'keyEmailHostName', '"+ serialize("myValue1") + "')").executeUpdate();
//        sessionFactory.getCurrentSession().createNativeQuery(
//                "INSERT INTO systemsetting (systemsettingid, name, value) VALUES " +
//                        "(10002, 'keyEmailUsername', '"+ serialize("myValue2") + "')").executeUpdate();
//        sessionFactory.getCurrentSession().createNativeQuery(
//                "INSERT INTO systemsetting (systemsettingid, name, value) VALUES " +
//                        "(10003, '" + EMAIL_PASSWORD.getName() + "', '"+ serialize(pbeStringEncryptor.encrypt("myValue3")) + "')").executeUpdate();
//
        jdbcTemplate.execute("INSERT INTO systemsetting (systemsettingid, name, value) VALUES " +
                "(10001, 'keyEmailHostName', '"+ serialize("myValue1") + "')");
        jdbcTemplate.execute(
                "INSERT INTO systemsetting (systemsettingid, name, value) VALUES " +
                        "(10002, 'keyEmailUsername', '"+ serialize("myValue2") + "')");
        jdbcTemplate.execute(
                "INSERT INTO systemsetting (systemsettingid, name, value) VALUES " +
                        "(10003, '" + EMAIL_PASSWORD.getName() + "', '"+ serialize(pbeStringEncryptor.encrypt("myValue3")) + "')");

        int threads = 10;

        ExecutorService service = Executors.newFixedThreadPool( threads );
//        ExecutorService service = Executors.newSingleThreadExecutor(  );

        Collection<Future<Map<String, Serializable>>> futures = new ArrayList<>( threads );
//        // warm hibernate
//        assertThat( systemSettingManager.getSystemSetting( EMAIL_HOST_NAME ), is("myValue1") );
//        assertThat( systemSettingManager.getSystemSetting( EMAIL_USERNAME ), is("myValue2") );
//        assertThat( systemSettingManager.getSystemSetting( EMAIL_PASSWORD ), is("myValue3") );

        for (int x = 0; x < 100000; x++) {
            for (int t = 0; t < threads; ++t) {
                futures.add(service.submit(() -> systemSettingManager
                        .getSystemSettings(
                                Lists.newArrayList(EMAIL_HOST_NAME, EMAIL_USERNAME, EMAIL_PASSWORD))));
            }
            System.out.println("10K !");
        }

        List<Serializable> vals = new ArrayList<>();

        for ( Future<Map<String, Serializable>> f : futures )
        {
            vals.add( f.get().get(EMAIL_HOST_NAME) );
            vals.add( f.get().get(EMAIL_USERNAME) );
            vals.add( f.get().get(EMAIL_PASSWORD) );
            //System.out.println(f.get());
        }
        System.out.println(vals.size());
        service.shutdown();
        //assertThat( vals.size(), equalTo( threads * 300000) );


    }

    private String serialize(String s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(s);
            out.flush();
            byte[] yourBytes = bos.toByteArray();
            return Hex.encodeHexString(yourBytes);
//            return Arrays.toString(yourBytes);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }


    }

}
