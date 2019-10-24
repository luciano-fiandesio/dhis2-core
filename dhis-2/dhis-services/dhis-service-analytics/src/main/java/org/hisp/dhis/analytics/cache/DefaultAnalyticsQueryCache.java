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

package org.hisp.dhis.analytics.cache;

import org.hisp.dhis.analytics.DataQueryParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Luciano Fiandesio
 */
@Component
public class DefaultAnalyticsQueryCache
    implements
    AnalyticsQueryCache
{

    private final JdbcTemplate jdbcTemplate;

    private final KeyBuilder keyBuilder;

    private final PeriodExtractor periodExtractor;

    private Map<Key, SqlRowSet> cache = new HashMap<>();

    public DefaultAnalyticsQueryCache( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate,
        DefaultKeyBuilder defaultKeyBuilder, PeriodExtractor periodExtractor )
    {
        checkNotNull( jdbcTemplate );
        checkNotNull( defaultKeyBuilder );
        checkNotNull( periodExtractor );
        this.jdbcTemplate = jdbcTemplate;
        this.keyBuilder = defaultKeyBuilder;
        this.periodExtractor = periodExtractor;
    }

    @Override
    public SqlRowSet execute( String sql, DataQueryParams params )
    {
        Double distance = this.periodExtractor.extract(params, TimeUnit.DAYS);

        // TODO calculate TTL based on distance (maybe a new component?)

        Key key = keyBuilder.build( sql );

        if ( cache.containsKey( key ) )
        {
            return cache.get( key );
        }
        else
        {
            SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet( sql );
            cache.put( key, sqlRowSet );
            return sqlRowSet;
        }
    }
}
