package org.hisp.dhis.cache.unit;

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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.cache.*;
import org.hisp.dhis.cache.key.TeiOuScopedUniqueValueCacheKey;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;

/**
 * @author Luciano Fiandesio
 */
public class TeiOuScopedUniqueAttributeCacheUnit
    extends
    DefaultCacheLoader<TrackedEntityAttributeService>
    implements
    CacheUnit<String, TeiOuScopedUniqueValueCacheKey>
{
    private static final Log log = LogFactory.getLog( TeiOuScopedUniqueAttributeCacheUnit.class );

    private String name;

    private long maxElements;

    private String regionName;

    private Cache<String> cache;

    public TeiOuScopedUniqueAttributeCacheUnit(String name, long maxElements, String regionName,
                                               TrackedEntityAttributeService trackedEntityAttributeService )
    {
        super( () -> trackedEntityAttributeService );
        this.name = name;
        this.maxElements = maxElements;
        this.regionName = regionName;

        cache = new SimpleCacheBuilder<String>().forRegion( "global" + regionName )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            // .withInitialCapacity( 10000 )
            .withMaximumSize( maxElements ).build();
    }

    @Override
    protected CacheInfo populateCache()
    {
        List<TrackedEntityAttribute> trackedEntityAttributes = this.dataSupplier.get()
            .getAllTrackedEntityAttributes();

        for ( TrackedEntityAttribute trackedEntityAttribute : trackedEntityAttributes )
        {
            
            if ( trackedEntityAttribute.getOrgUnitScopeNullSafe() )
            {

                try
                {
                    Map<String, String> uniqueValueForTrackedEntityAttribute = this.dataSupplier.get()
                        .getUniqueValueForTrackedEntityAttributeMap( trackedEntityAttribute );

                    for ( String key : uniqueValueForTrackedEntityAttribute.keySet() )
                    {
                        cache.put(
                            new TeiOuScopedUniqueValueCacheKey( trackedEntityAttribute, createOrgUnitWithUid( key ) )
                                .get(),
                            uniqueValueForTrackedEntityAttribute.get( key ) );
                    }

                }
                catch ( Exception e )
                {
                    log.error( "An error occurred during Tracked Entity Attribute unique value population.", e );
                }
            }
        }

        return new CacheInfo( name, (long) cache.getAll().size(), Boolean.class.getName(),
            this.dataSupplier.get().getClass().getName() );

    }
    
    private OrganisationUnit createOrgUnitWithUid( String uid )
    {
        OrganisationUnit ou = new OrganisationUnit();
        ou.setUid( uid );
        return ou;
    }

    public String getName()
    {
        return name;
    }

    public long getMaxElements()
    {
        return maxElements;
    }

    public String getRegionName()
    {
        return regionName;
    }

    @Override
    public String get( TeiOuScopedUniqueValueCacheKey key )
    {
        return cache.get( key.get() ).orElse( null );
    }
    
}
