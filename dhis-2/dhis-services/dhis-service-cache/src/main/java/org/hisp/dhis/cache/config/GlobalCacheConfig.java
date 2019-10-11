package org.hisp.dhis.cache.config;

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

import java.util.Map;

import org.hisp.dhis.cache.CacheLoader;
import org.hisp.dhis.cache.CacheUnit;
import org.hisp.dhis.cache.unit.OrgUnitCacheUnit;
import org.hisp.dhis.cache.unit.TeiOuScopedUniqueAttributeCacheUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class GlobalCacheConfig
{

    @Bean
    public OrgUnitCacheUnit orgUnitCacheUnit( OrganisationUnitService organisationUnitService )
    {
        return new OrgUnitCacheUnit( "orgUnit", 100_000, "orgUnit", organisationUnitService );
    }

    @Bean
    public TeiOuScopedUniqueAttributeCacheUnit teiUniqueAttributeCacheUnit(
        TrackedEntityAttributeService trackedEntityAttributeService )
    {
        return new TeiOuScopedUniqueAttributeCacheUnit( "teaUnique", 1000, "teaUnique", trackedEntityAttributeService );
    }

    @Bean( "cacheUnitsMap" )
    public Map<Class<? extends CacheUnit>, CacheLoader> cacheUnitsMap(OrgUnitCacheUnit orgUnitCacheUnit,
                                                                      TeiOuScopedUniqueAttributeCacheUnit teiOuScopedUniqueAttributeCacheUnit)
    {
        return ImmutableMap.<Class<? extends CacheUnit>, CacheLoader> builder()
            .put( orgUnitCacheUnit.getClass(), orgUnitCacheUnit )
            .put( teiOuScopedUniqueAttributeCacheUnit.getClass(), teiOuScopedUniqueAttributeCacheUnit).build();
    }

}
