package dev.sympho.bot_utils;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.common.ReactorResources;
import io.netty.resolver.dns.DnsNameResolverTimeoutException;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

/**
 * Utilities for using HTTP clients.
 *
 * @version 1.0
 * @since 1.0
 * @apiNote Useful for configuring the HTTP clients used for Discord.
 */
@SuppressWarnings( "MultipleStringLiterals" )
public final class HttpClientUtils {

    /** The default DNS retry policy. */
    @SuppressWarnings( "MagicNumber" )
    public static final Retry DEFAULT_DNS_RETRY_POLICY = Retry.backoff( 5, Duration.ofSeconds( 1 ) )
            .maxBackoff( Duration.ofSeconds( 10 ) )
            .filter( ex -> ex instanceof DnsNameResolverTimeoutException )
            .onRetryExhaustedThrow( ( retryBackoffSpec, retrySignal ) -> retrySignal.failure() );

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger( HttpClientUtils.class );

    /**
     * Endpoint types used to interact with Discord.
     *
     * @since 1.0
     */
    public enum EndpointType {

        /** The REST API. */
        REST( () -> {
            final var idPattern = Pattern.compile( "(?<=/)\\d++(?=/|$)" );
            return s -> idPattern.matcher( s ).replaceAll( "{id}" );
        } ),

        /** Attachment files. */
        ATTACHMENT( () -> p -> p.startsWith( "/attachments/" )
                ? "/attachments/{message}/{id}/{file}" 
                : p 
        );

        /** The URI tag formatter. */
        final UnaryOperator<String> uriTagFormatter;

        /**
         * Creates a new instance.
         *
         * @param uriTagFormatter The URI formatter to use for metric tags.
         */
        EndpointType( final Supplier<UnaryOperator<String>> uriTagFormatter ) {

            this.uriTagFormatter = uriTagFormatter.get();

        }

        /**
         * Retrieves the URI formatter to use for metric tags.
         *
         * @return The formatter.
         */
        public UnaryOperator<String> uriTagFormatter() {
            return this.uriTagFormatter;
        }

        /**
         * Creates and configures an HTTP client to use with this endpoint type.
         *
         * @param dnsRetryPolicy The DNS retry policy to use.
         * @return The configured client.
         */
        public HttpClient configureClient( final Retry dnsRetryPolicy ) {

            var client = ReactorResources.DEFAULT_HTTP_CLIENT.get();
            client = addDnsRetry( client, dnsRetryPolicy );
            client = enableMetrics( client, this );
            return client;

        }

    }

    /** Do not instantiate. */
    private HttpClientUtils() {}

    /**
     * Determines if a given DNS exception was caused by a timeout, extracting the timeout
     * exception if it is.
     *
     * @param exception The DNS exception.
     * @return The underlying timeout exception if any, otherwise the given exception.
     */
    private static Throwable extractDnsTimeout( final UnknownHostException exception ) {

        return exception.getCause() instanceof DnsNameResolverTimeoutException
                ? exception.getCause()
                : exception;

    }

    /**
     * Creates a function that adds DNS retry capabilities to a connection mono.
     *
     * @param retryPolicy The retry policy to use.
     * @return The conversion function.
     * @apiNote Use with {@link HttpClient#mapConnect(java.util.function.Function)}.
     */
    public static Function<Mono<? extends Connection>, Mono<? extends Connection>> addDnsRetry(
            final Retry retryPolicy ) {

        return src -> src.onErrorMap( 
                        UnknownHostException.class, 
                        HttpClientUtils::extractDnsTimeout 
                )
                .doOnError( DnsNameResolverTimeoutException.class, ex -> LOGGER.warn(
                        "DNS query {} timed out", ex.question()
                ) )
                .retryWhen( DEFAULT_DNS_RETRY_POLICY )
                .doOnError( DnsNameResolverTimeoutException.class, ex -> LOGGER.error(
                        "DNS query {} reached the retry limit", ex.question()
                ) )
                .doOnSubscribe( s -> LOGGER.trace( "Hooking on connection" ) );

    }

    /**
     * Adds DNS retry capabilities to an HTTP client.
     *
     * @param client The client to configure.
     * @param retryPolicy The retry policy to use.
     * @return The configured client.
     * @apiNote This is a convenience shortcut for calling 
     *          {@link HttpClient#mapConnect(java.util.function.Function)}
     *          with the result of {@link #addDnsRetry(Retry)}; it is equivalent to 
     *          {@code client.mapConnect(HttpClientUtils.addDnsRetry(retryPolicy))}.
     */
    public static HttpClient addDnsRetry( final HttpClient client, final Retry retryPolicy ) {

        return client.mapConnect( addDnsRetry( retryPolicy ) );

    }

    /**
     * Adds DNS retry capabilities to an HTTP client, using the 
     * {@link #DEFAULT_DNS_RETRY_POLICY default retry policy}.
     *
     * @param client The client to configure.
     * @return The configured client.
     * @see #addDnsRetry(HttpClient, Retry)
     * @see #DEFAULT_DNS_RETRY_POLICY
     * @apiNote This is equivalent to 
     *       {@code HttpClientUtils.addDnsRetry(client, HttpClientUtils.DEFAULT_DNS_RETRY_POLICY)}.
     */
    public static HttpClient addDnsRetry( final HttpClient client ) {

        return addDnsRetry( client, DEFAULT_DNS_RETRY_POLICY );

    }

    /**
     * Enables metrics on an HTTP client.
     *
     * @param client The client to configure.
     * @param type The type of endpoint being used with that client.
     * @return The configured client.
     */
    public static HttpClient enableMetrics( final HttpClient client, final EndpointType type ) {

        return client.metrics( true, type.uriTagFormatter() );

    }
    
}
