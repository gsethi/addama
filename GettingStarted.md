# Introduction #
To create an Addama domain running on Google App Engine, users will need to:
  1. Become familiar with the Google App Engine (GAE) platform http://code.google.com/appengine/kb/
  1. [Deploy Addama on GAE](#Deploy_Addama_Registry.md)
  1. [Deploy Local Services on HTTP Server](#Deploy_Local_Services.md)
  1. [Register Custom Web Application](#Register_Custom_Web_Application.md)
## Deploy Addama Registry ##

To deploy your _Addama Registry_ to **[Google App Engine](http://appengine.google.com)**, you will need to complete the following steps:

  1. Install Google App Engine (GAE) Java SDK: http://code.google.com/appengine/docs/java/gettingstarted/installing.html
  1. Create a new GAE Appspot at https://appengine.google.com/start/createapp
  1. Download distribution of Addama 3.0 from http://code.google.com/p/addama/downloads/list
  1. Create appengine-web.xml and place in war/WEB-INF/ ... See example below:
```
<?xml version="1.0" encoding="utf-8"?>
<appengine-web-app xmlns="http://appengine.google.com/ns/1.0">
    <application>ENTER Application Identifier HERE</application>
    <version>1</version>
    <inbound-services>
        <service>channel_presence</service>
    </inbound-services>
    <system-properties>
        <property name="java.util.logging.config.file" value="WEB-INF/logging.properties"/>
    </system-properties>
</appengine-web-app>
```
  1. Execute appcfg.sh update war
    * appcfg.sh - refers to script in Java SDK
    * war - refers to package inside software distribution for Addama

## Deploy Local Services ##
**TODO: coming soon**

## Register Custom Web Application ##
**TODO: coming soon**