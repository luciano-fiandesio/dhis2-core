package org.hisp.dhis.flyway.configuration;

import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.hisp.dhis.common.DhisApiVersion;

public class DhisFlywayConfiguration extends ClassicConfiguration
{
    private String installedBy = String.valueOf( DhisApiVersion.DEFAULT.getVersion() );

    @Override
    public String getInstalledBy()
    {
        return installedBy;
    }
}
