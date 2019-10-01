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

package org.hisp.dhis.dxf2.mapping;

import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Coordinate;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * @author Luciano Fiandesio
 */
@Mapper
public abstract class EnrollmentProgramInstanceMapper
{
    @Mapping( source = "uid", target = "enrollment" )
    @Mapping( source = "entityInstance.trackedEntityType.uid", target = "trackedEntityType" )
    @Mapping( source = "entityInstance.uid", target = "trackedEntityInstance" )
    @Mapping( source = "organisationUnit.uid", target = "orgUnit" )
    @Mapping( source = "organisationUnit.name", target = "orgUnitName" )
    @Mapping( source = "program.uid", target = "program" )
    @Mapping( source = "endDate", target = "completedDate" )
    @Mapping( target = "created", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programInstance.getCreated() ) )" )
    @Mapping( target = "createdAtClient", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programInstance.getCreatedAtClient() ) )" )
    @Mapping( target = "lastUpdated", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programInstance.getLastUpdated() ) )" )
    @Mapping( target = "lastUpdatedAtClient", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programInstance.getLastUpdatedAtClient() ) )" )
    @Mapping( target = "notes", source = "comments")

    public abstract Enrollment programInstanceToEnrollment( ProgramInstance programInstance );

    @Mapping( source = "uid", target = "note" )
    @Mapping( source = "commentText", target = "value" )
    @Mapping( source = "creator", target = "storedBy" )
    @Mapping( target = "storedDate", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( comment.getCreated() ) )" )
    public abstract Note trackedEntityCommentToNote(TrackedEntityComment comment);

    @AfterMapping
    protected void mapCoordinateIfPoint( ProgramInstance programInstance, @MappingTarget Enrollment enrollment )
    {
        if ( programInstance.getProgram() != null && programInstance.getProgram().getFeatureType() != null )
        {
            if ( programInstance.getProgram().getFeatureType().equals( FeatureType.POINT ) )
            {
                com.vividsolutions.jts.geom.Coordinate co = programInstance.getGeometry().getCoordinate();
                enrollment.setCoordinate( new Coordinate( co.x, co.y ) );
            }
        }
    }
}
