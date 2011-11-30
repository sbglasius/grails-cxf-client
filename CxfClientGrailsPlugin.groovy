import com.grails.cxf.client.DynamicWebServiceClient

class CxfClientGrailsPlugin {
    // the plugin version
    def version = "1.2.3"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "test/**"
    ]

    def license = "APACHE"
    def author = "Christian Oestreich"
    def authorEmail = "acetrike@gmail.com"
    def developers = [
            [name: "Christian Oestreich", email: "acetrike@gmail.com"],
            [name: "Brett Borchardt", email: "bborchardt@gmail.com"]]
    def title = "Cxf Client - Support for CXF and JAXB Soap Clients"
    def description = '''\\
Used for easily integrating existing or new cxf/jaxb web service client code with soap services.  Also provides wsdl2java grails target to easily generate code into srv/java from configured cxf clients.
'''

    // URL to the plugin's documentation
    def documentation = "https://github.com/ctoestreich/cxf-client"
    def scm = [url: "https://github.com/ctoestreich/cxf-client"]

    def watchedResources = [
            "file:${getPluginLocation()}/grails-app/services/**/*Service.groovy",
            "file:${getPluginLocation()}/grails-app/controllers/**/*Controller.groovy",
            "file:${getPluginLocation()}/grails-app/taglib/**/*TagLib.groovy"
    ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {

        webServiceClientFactory(com.grails.cxf.client.WebServiceClientFactoryImpl)

        log.info "wiring up cxf-client beans"

        def cxfClientConfigMap = application.config?.cxf?.client ?: [:]

        cxfClientConfigMap.each { cxfClient ->
            def cxfClientName = cxfClient.key
            def client = application.config?.cxf?.client[cxfClientName]
            def inInterceptorList = []
            def outInterceptorList = []
            def outFaultInterceptorList = []

            log.info "wiring up client for $cxfClientName [clientInterface=${client?.clientInterface} and serviceEndpointAddress=${client?.serviceEndpointAddress}]"

            //the call to the map returns a gstring as [:] not as a map literal (fix this?)
            if(!client?.clientInterface || (client.serviceEndpointAddress == "[:]")) {
                String errorMessage = "Web service client $cxfClientName cannot be created before setting the clientInterface=${client?.clientInterface} and serviceEndpointAddress=${client?.serviceEndpointAddress} properties"
                println errorMessage
                log.error errorMessage
            }

            if(client?.secured && !client?.securityInterceptor) {
                "securityInterceptor${cxfClientName}"(com.grails.cxf.client.security.DefaultSecurityOutInterceptor) {
                    username = client?.username ?: ""
                    password = client?.password ?: ""
                }
            }

            "${cxfClientName}"(DynamicWebServiceClient) {
                webServiceClientFactory = ref("webServiceClientFactory")
                if(client?.secured || client?.securityInterceptor) {
                    if(client?.securityInterceptor) {
                        outInterceptorList << ref("${client.securityInterceptor}")
                    } else {
                        outInterceptorList << ref("securityInterceptor${cxfClientName}")
                    }
                }
                if(client?.inInterceptors) {
                    client.inInterceptors.split(',').each {
                        inInterceptorList << ref(it)
                    }
                }
                if(client?.outInterceptors) {
                    client.outInterceptors.split(',').each {
                        outInterceptorList << ref(it)
                    }
                }
                if(client?.outFaultInterceptors) {
                    client.outFaultInterceptors.split(',').each {
                        outFaultInterceptorList << ref(it)
                    }
                }
                inInterceptors = inInterceptorList
                outInterceptors = outInterceptorList
                outFaultInterceptors = outFaultInterceptorList
                clientInterface = client.clientInterface ?: ""
                serviceName = cxfClientName
                serviceEndpointAddress = client?.serviceEndpointAddress ?: ""
                secured = (client?.secured || client?.securityInterceptor) ?: false
            }
        }

        log.info "completed mapping cxf-client beans"
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->

    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    ConfigObject getBuildConfig() {
        GroovyClassLoader classLoader = new GroovyClassLoader(getClass().getClassLoader())
        return new ConfigSlurper().parse(classLoader.loadClass('BuildConfig'))
    }

    String getPluginLocation() {
        return getBuildConfig()?.grails?.plugin?.location?.'cxf-client'
    }
}
