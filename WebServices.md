# Introduction #
Addama defines standardized REST+JSON web service interfaces to facilitate integration with different types of data management systems like File Systems, Relational Databases, Search Indexes, Chromosomal Datasources, as well as a Job Execution interface for simple integration of command-line executables.

# Configuration #
All of the provided "local services" (running outside of Google App Engine) have a similar configuration and deployment procedure.  These services are deployed within a standard J2EE web app container (such as Apache Tomcat).  Each is configured via a JSON configuration file placed on the `CLASSPATH` of the web application container.  An **optional** `addama.properties` file, also placed on the `CLASSPATH` will be used by the services to register with an Addama registry running on Google App Engine.

### Example Configuration ###
```
{
    family: "/addama/classification",  // identifies the base URI for the service mappings
    label: "Some top level label",
    mappings: [
        {
            id: "unique_mapping_id",  // used as part of the URI :: /addama/classification/unique_mapping_id
            label: "Some mapping label"
            // additional configuration of individual mappings  can be placed here
        }
    ]
    // additional configuration for the service can be placed here
}
```

# Installation #
A standard deployment of local web services using Apache Tomcat is provided in the [Downloads](http://code.google.com/p/addama/downloads/list) section.

# Web Services #
The following pages provide more specific detail on web service interfaces and configuration
  * [Chromosome Index Service](WebServicesChromosomeIndex.md)
  * [Google Datasources API Service](WebServicesGoogleDSApi.md)
  * [Filesystem Workspace/Repository Service](WebServicesFSWorkspaces.md)
  * [Script Execution Service](WebServicesScriptExecution.md)