package tukano.impl.rest;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Application;
import srv.Authentication;
//import srv.ControlResource;
import srv.auth.RequestCookiesCleanupFilter;
import srv.auth.RequestCookiesFilter;
import utils.Args;
import tukano.impl.Token;

public class TukanoRestServer extends Application {
	final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());

	static final String INETADDR_ANY = "0.0.0.0";
	public static String serverURI;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}

	private Set<Object> singletons = new HashSet<>();
	private Set<Class<?>> resources = new HashSet<>();


	public TukanoRestServer() {
		serverURI = "";
		Token.setSecret(Args.valueOf("-secret", "spotingale"));
		resources.add(RestBlobsResource.class);
		resources.add(RestUsersResource.class);
		resources.add(RestShortsResource.class);

		//resources.add(ControlResource.class);
		
		resources.add(RequestCookiesFilter.class);
     	resources.add(RequestCookiesCleanupFilter.class);
        resources.add(Authentication.class);
	}

	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	public static void main(String[] args) throws Exception {
		serverURI = "http://127.0.0.1:8080/tukano/rest";
		Args.use(args);
		new TukanoRestServer();
	}
}