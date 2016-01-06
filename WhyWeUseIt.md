## Scalability, Availability, Accessibility ##
The purpose of a cloud service platform is to allow software applications to scale to any volume of requests; be highly available to service any number of clients; and to provide the most optimal access over the network regardless of the user's location.

An application running on the cloud is able to scale to large demands without placing undue strain on the computational resources of an enterprise (e.g. web and application servers, network routers) or on its IT department.

## Reduce Software Complexity ##
At the core of Addama is a registry that dispatches all service requests.  These requests can originate from user interfaces (desktop or web applications), formalized analysis tools (pipelines, cron jobs) and ad-hoc scripts (Matlab, R, Python).  Through the registry we can ensure that all of these requests are properly authenticated and authorized, therefore reducing the complexity for each registered service.

Services need not authenticate and authorize users, they only need to establish a trust relationship with the registry.

This deployment strategy places a significant amount of traffic through the registry, making App Engine a practical platform to serve the registry.

## But, Where is the Data? ##
There are a many reasons why an enterprise may be reluctant to move their data to the cloud.  Most notable are concerns over security, privacy, cost and vendor lock-in.

The Addama software architecture offers a different approach.  The data does not need to be stored on the cloud.  Indexes and links to the data are made available through the Addama registry.

Files are not served through the registry, but Addama helps to establish a trusted direct connection between client and service.  The service has a variety of choices when serving data files (e.g. HTTP, FTP), however depending on the sensitivity of certain data types (e.g. clinical patient data) or the size (+100GBs), it may be more practical for the service to present a local link to a file that is resolved within an enterprise firewall.