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

package org.hisp.dhis.random;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.geojson.geom.GeometryJSON;
import org.jeasy.random.api.Randomizer;


/**
 * @author Luciano Fiandesio
 */
public class GeometryRandomizer
    implements
        Randomizer<Geometry>
{
    private GeometryJSON geometryJSON = new GeometryJSON();
    private final static String POINT = "{\n" + "\"type\": \"Point\",\n" + "\"coordinates\": [%s]\n" + "}";

    private List<String> points = Arrays.asList(
            "4.921875, 24.206889622398023",
            "19.6875,14.26438308756265",
            "22.148437499999996,-10.487811882056683",
            "-11.513671874999998,13.239945499286312" );

    @Override
    public Geometry getRandomValue()
    {
        try
        {
           return geometryJSON
                .read( String.format( POINT, points.get( new Random().nextInt( points.size() - 1 ) ) ) );
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException();
        }
    }
}
