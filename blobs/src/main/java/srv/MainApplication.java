package srv;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import srv.auth.RequestCookiesCleanupFilter;
import srv.auth.RequestCookiesFilter;

public class MainApplication extends Application
{
	private Set<Object> singletons = new HashSet<>();
	private Set<Class<?>> resources = new HashSet<>();

	public MainApplication() {
		resources.add(ControlResource.class);
		
		resources.add(RequestCookiesFilter.class);
     	resources.add(RequestCookiesCleanupFilter.class);
        resources.add(Authentication.class);
        
	}

	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
