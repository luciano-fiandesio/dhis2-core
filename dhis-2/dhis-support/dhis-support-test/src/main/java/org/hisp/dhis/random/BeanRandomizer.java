package org.hisp.dhis.random;

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

import com.vividsolutions.jts.geom.Geometry;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.period.PeriodType;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jeasy.random.FieldPredicates.*;

/**
 * @author Luciano Fiandesio
 */
public class BeanRandomizer
{
    private EasyRandom rand;

    private EasyRandomParameters DEFAULTS = new EasyRandomParameters()
        .seed( 84782783L )
        .objectPoolSize( 100 )
        .randomizationDepth( 3 )
        .charset( StandardCharsets.UTF_8 )
        .stringLengthRange( 5, 50 )
        .collectionSizeRange( 5, 20 )
        .scanClasspathForConcreteTypes( true )
        .overrideDefaultInitialization( true )
        .ignoreRandomizationErrors( false );

    public BeanRandomizer()
    {
        DEFAULTS.randomize( PeriodType.class, new PeriodTypeRandomizer() );
        DEFAULTS.randomize( named( "uid" ).and( ofType( String.class ) ), new UidRandomizer() );
        DEFAULTS.randomize( named( "id" ).and( ofType( long.class ) ), new IdRandomizer() );
        DEFAULTS.randomize( named( "geometry" ).and( ofType( Geometry.class ) ), new GeometryRandomizer() );
        // Exclusions //
        DEFAULTS.excludeField( named( "translations" ) );
        DEFAULTS.excludeField( named( "translationCache" ) );
        DEFAULTS.excludeField( named( "externalAccess" ) );
        DEFAULTS.excludeField( named( "publicAccess" ) );
        DEFAULTS.excludeField( named( "user" ) );
        DEFAULTS.excludeField( named( "userAccesses" ) );
        DEFAULTS.excludeField( named( "userGroupAccesses" ) );
        DEFAULTS.excludeField( named( "access" ) );
        DEFAULTS.excludeField( named( "favorites" ) );
        DEFAULTS.excludeField( named( "href" ) );
        DEFAULTS.excludeField( named( "user" ) );
        DEFAULTS.excludeField( named( "textPattern" ) );
        DEFAULTS.excludeField( named( "cacheAttributeValues" ) );

        rand = new EasyRandom( DEFAULTS );
    }

    /**
     * Generates an instance of the specified type and fill the instance's properties with random data
     * @param type The bean type
     *
     * @return an instance of the specified type
     */
    public <T> T randomObject( final Class<T> type )
    {
        return rand.nextObject( type );
    }

    /**
     * Generates multiple instances of the specified type and fills each instance's properties with random data
     * @param type The bean type
     * @param amount the amount of beans to generate
     *
     * @return an instance of the specified type
     */
    public <T> List<T> randomObjects( final Class<T> type, int amount)
    {
        return rand.objects( type, amount ).collect( Collectors.toList() );
    }
}
