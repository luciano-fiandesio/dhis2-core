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

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.event.Coordinate;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.mapstruct.*;

/**
 * @author Luciano Fiandesio
 */
@Mapper(uses = { TrackedEntityCommentNoteMapper.class })
public abstract class EventProgramStageInstanceMapper
{
    @Mapping( source = "uid", target = "event" )
    @Mapping( source = "programInstance.entityInstance.uid", target = "trackedEntityInstance" )
    @Mapping( source = "programInstance.followup", target = "followup" )
    // event.setEnrollmentStatus( EnrollmentStatus.fromProgramStatus( programStageInstance.getProgramInstance().getStatus() ) );
    @Mapping( target = "eventDate", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programStageInstance.getExecutionDate() ) )" )
    @Mapping( target = "dueDate", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programStageInstance.getDueDate() ) )" )
    @Mapping( target = "completedDate", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programStageInstance.getCompletedDate() ) )" )
    @Mapping( target = "created", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programStageInstance.getCreated() ) )" )
    @Mapping( target = "createdAtClient", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programStageInstance.getCreatedAtClient() ) )" )
    @Mapping( target = "lastUpdated", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programStageInstance.getLastUpdated() ) )" )
    @Mapping( target = "lastUpdatedAtClient", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( programStageInstance.getLastUpdatedAtClient() ) )" )
    @Mapping( source = "assignedUser.uid" ,target = "assignedUser" )
    @Mapping( source = "assignedUser.username", target = "assignedUserUsername" )
    @Mapping( source = "organisationUnit.uid", target = "orgUnit" )
    @Mapping( source = "organisationUnit.name", target = "orgUnitName" )
    @Mapping( source = "programInstance.program.uid", target = "program" )
    @Mapping( source = "programStageInstance.programInstance.uid", target = "enrollment" )
    @Mapping( source = "programStageInstance.programStage.uid", target = "programStage" )
    @Mapping( source = "attributeOptionCombo.uid", target = "attributeOptionCombo" )
//    event.setAttributeCategoryOptions( String.join( ";", programStageInstance.getAttributeOptionCombo()
//            .getCategoryOptions().stream().map(CategoryOption::getUid ).collect(Collectors.toList() ) ) );
    @Mapping( target = "dataValues", expression = "java( eventDataValuesToDataValues(this.getDataValues( programStageInstance, isSynchronizationQuery ) ) )" )
    public abstract Event programStageInstanceToEvent(ProgramStageInstance programStageInstance, @Context boolean isSynchronizationQuery  );

    @Mapping( target = "created", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( eventDataValue.getCreated() ) )" )
    @Mapping( target = "lastUpdated", expression = "java( org.hisp.dhis.util.DateUtils.getIso8601NoTz( eventDataValue.getLastUpdated() ) )" )
    public abstract DataValue eventDataValueToDataValue(EventDataValue eventDataValue );

    public abstract Set<DataValue> eventDataValuesToDataValues(Set<EventDataValue> eventDataValues);

    @AfterMapping
    protected void mapCoordinateIfPoint( ProgramStageInstance psi, @MappingTarget Event event, @Context boolean isSynchronizationQuery )
    {
        // Lat and lnt deprecated in 2.30, remove by 2.33
        if ( psi.getGeometry() != null && psi.getGeometry().getGeometryType().equals( "Point" ) )
        {
            com.vividsolutions.jts.geom.Coordinate geometryCoordinate = psi.getGeometry().getCoordinate();
            event.setCoordinate( new Coordinate( geometryCoordinate.x, geometryCoordinate.y ) );
        }
    }

    protected Set<EventDataValue> getDataValues(ProgramStageInstance psi, boolean isSynchronizationQuery) {

        Set<EventDataValue> dataValues;
        if ( !isSynchronizationQuery )
        {
            dataValues = psi.getEventDataValues();
        }
        else
        {
            // collect uids to sync
            Set<String> dataElementsToSync = psi.getProgramStage().getProgramStageDataElements().stream()
                    .filter( psde -> !psde.getSkipSynchronization() )
                    .map( psde -> psde.getDataElement().getUid() )
                    .collect( Collectors.toSet());

            dataValues = psi.getEventDataValues().stream()
                    .filter( dv -> dataElementsToSync.contains( dv.getDataElement() ) )
                    .collect( Collectors.toSet());
        }
        return dataValues;
    }
}
