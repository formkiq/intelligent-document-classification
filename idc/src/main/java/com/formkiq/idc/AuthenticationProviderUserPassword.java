package com.formkiq.idc;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@Singleton
public class AuthenticationProviderUserPassword implements AuthenticationProvider {

	@Override
	public Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest,
			AuthenticationRequest<?, ?> authenticationRequest) {
		
		return Flux.create(emitter -> {
			
			String username = System.getProperty("api.username");
			String password = System.getProperty("api.password");
			
			if (username == null) {
				username = System.getenv("api.username");
			}
			
			if (password == null) {
				password = System.getenv("api.password");
			}
						
			if (authenticationRequest.getIdentity().equals(username)
					&& authenticationRequest.getSecret().equals(password)) {
				emitter.next(AuthenticationResponse.success((String) authenticationRequest.getIdentity()));
				emitter.complete();
			} else {
				emitter.error(AuthenticationResponse.exception());
			}
		}, FluxSink.OverflowStrategy.ERROR);
	}
}