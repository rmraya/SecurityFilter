# SecurityFilter

Custom security filter for Tomcat 9 to be used when serving static content

## Installation

1. Clone this repository
2. Copy SecurityFilter.jar to Tomcat's `/lib` folder
3. Edit Tomcat's `conf/web.xml` and add the following lines in Filters section:

```xml
  <filter>
    <filter-name>SecurityFilter</filter-name>
    <filter-class>com.maxprograms.filter.SecurityFilter</filter-class>
  </filter>

  <filter-mapping>
    <filter-name>SecurityFilter</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
  </filter-mapping>
```
