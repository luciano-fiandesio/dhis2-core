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
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.cache.*;
import org.hisp.dhis.cache.key.SingleValueCacheKey;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;

/**
 * @author Luciano Fiandesio
 */
public class OrgUnitCacheUnit
    extends
        DefaultCacheLoader<OrganisationUnitService>
    implements
        CacheUnit<OrganisationUnit, SingleValueCacheKey>
{

    private String name;

    private long maxElements;

    private String regionName;

    private Cache<OrganisationUnit> cache;

    public OrgUnitCacheUnit(String name, long maxElements, String regionName,
                            OrganisationUnitService organisationUnitService )
    {
        super( () -> organisationUnitService );
        this.name = name;
        this.maxElements = maxElements;
        this.regionName = regionName;

        cache = new SimpleCacheBuilder<OrganisationUnit>().forRegion( "global" + regionName )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            // .withInitialCapacity( 10000 )
            .withMaximumSize( maxElements ).build();
    }

    @Override
    protected CacheInfo populateCache()
    {
        List<OrganisationUnit> orgUnits = this.dataSupplier.get().getAllOrganisationUnits();

        for ( OrganisationUnit orgUnit : orgUnits )
        {
            this.cache.put( orgUnit.getUid(), orgUnit );
        }

        return new CacheInfo( name, (long) cache.getAll().size(), OrganisationUnit.class.getName(),
            this.dataSupplier.get().getClass().getName() );

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
    public OrganisationUnit get( SingleValueCacheKey key )
    {
        return cache.get( key.get() ).orElse( null );
    }

    @Override
    public OrganisationUnit getAndPutIfMissing( SingleValueCacheKey key, OrganisationUnit value )
    {

        return CacheUnitUtils.getAndPut( this.cache, key, value );
    }
}
