## Authentication Options ##
An **Addama** instance offers access to any registered service using authentication options provided by Google App Engine APIs.  There are **three** options for configuring access to a GAE **appspot**.  These options are provided by Google App Engine, and Addama works with any of these configurations.

**See [this page](WhatUpWithGoogleAccounts.md) for more information on these options.**

  * **Option 1:  Google Account** - Allows anyone that has a Google Account
    * Google Accounts include gmail (e.g. user@gmail.com), but can also be created using any email address (e.g. user@example.org)
      * Google Accounts can be created at:  https://www.google.com/accounts/NewAccount
    * Addama requires that users sign-on to access data, but is generally open
    * Example:  https://addama-systemsbiology-public.appspot.com - Public registry for demos, reference data, visualizations and tools for use by the research community.

  * **Option 2:  Google Apps** - Allows users of a specific Google Apps domain (e.g. systemsbiology.org)
    * ISB has had a domain for over 4 years providing access to Gmail, Calendar, Docs, etc
    * Example:  https://addama-systemsbiology.appspot.com - Domain registry for tools, data, visualizations accessible only by ISB users (e.g. user@systemsbiology.org)
    * Our IT department fully controls access to this domain.  When a researcher leaves ISB, their access to all apps (e.g. Gmail, Docs, Addama) in the Google Apps domain is revoked.

  * **Option 3:  Federated OpenID** -  Allows users associated with an alternative OpenID provider
    * this is newer feature, but it essentially allows administrators to specify an OpenID provider other than Google (e.g. Yahoo, Twitter)
    * More information at: http://code.google.com/appengine/articles/openid.html

Regardless of the chosen authentication option, Addama is **absolutely never** given access to a user's password.

## Greenlist Access Control ##
In addition to the options listed above, Addama provides administrators with a Greenlist to control access to their application.  Administrators can maintain a list of email addresses for users that will be allowed to access their Google App Engine application.
  * Example:  https://cancerregulome.appspot.com - Private registry for a select group of collaborators within our GDAC at ISB and MD Anderson (using email addresses from ISB, MD Anderson, and Gmail).  This domain is only available to a subset of users at ISB and MD Anderson.

## Additional Concerns ##
The Addama service registry enforces that all communication between clients, registry and services happen over **HTTPS**.

## Programmatic Access ##
Addama provides securely generated API keys and [simple scripts](http://code.google.com/p/addama/source/browse/#hg/clients/src/main/python) for programmatic access.  Users can obtain a personal API Key only through an Addama user interface (must be logged in).  Any program making HTTP calls to Addama must include the API key in a request header (x-addama-apikey).  API keys are associated with a particular user, they carry the same permissions as the user, and can be revoked at any time by administrators.