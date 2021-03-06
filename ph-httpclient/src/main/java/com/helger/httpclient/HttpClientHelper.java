/**
 * Copyright (C) 2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.httpclient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.http.EHTTPMethod;
import com.helger.network.proxy.HttpProxyConfig;

/**
 * Some utility methods for creating and handling Apache httpclient objects.
 *
 * @author Philip Helger
 */
public final class HttpClientHelper
{
  private HttpClientHelper ()
  {}

  @Nonnull
  public static HttpRequestBase createRequest (@Nonnull final EHTTPMethod eHTTPMethod,
                                               @Nonnull final ISimpleURL aSimpleURL)
  {
    final String sURL = aSimpleURL.getAsStringWithEncodedParameters ();
    switch (eHTTPMethod)
    {
      case DELETE:
        return new HttpDelete (sURL);
      case GET:
        return new HttpGet (sURL);
      case HEAD:
        return new HttpHead (sURL);
      case OPTIONS:
        return new HttpOptions (sURL);
      case TRACE:
        return new HttpTrace (sURL);
      case POST:
        return new HttpPost (sURL);
      case PUT:
        return new HttpPut (sURL);
      default:
        throw new IllegalStateException ("Unsupported HTTP method: " + eHTTPMethod);
    }
  }

  @Nullable
  public static ContentType createContentType (@Nullable final String sContentType, @Nullable final Charset aCharset)
  {
    if (StringHelper.hasNoText (sContentType))
      return null;
    return ContentType.create (sContentType, aCharset);
  }

  @Nonnull
  public static Charset getCharset (@Nonnull final ContentType aContentType)
  {
    final Charset ret = aContentType.getCharset ();
    return ret != null ? ret : HTTP.DEF_CONTENT_CHARSET;
  }

  @Nullable
  public static HttpHost createHttpHost (@Nullable final Proxy aProxy)
  {
    if (aProxy != null && aProxy.type () == Proxy.Type.HTTP)
    {
      if (aProxy.address () instanceof InetSocketAddress)
      {
        final InetSocketAddress aISA = (InetSocketAddress) aProxy.address ();
        return new HttpHost (aISA.getHostName (), aISA.getPort ());
      }
    }
    return null;
  }

  @Nullable
  public static HttpHost createHttpHost (@Nullable final HttpProxyConfig aProxyConfig)
  {
    if (aProxyConfig != null)
      return new HttpHost (aProxyConfig.getHost (), aProxyConfig.getPort ());
    return null;
  }

  @Nullable
  public static Credentials createCredentials (@Nullable final HttpProxyConfig aProxyConfig)
  {
    if (aProxyConfig != null && aProxyConfig.hasUserNameOrPassword ())
      return new UsernamePasswordCredentials (aProxyConfig.getUserName (), aProxyConfig.getPassword ());
    return null;
  }

  @Nonnull
  public static HttpContext createHttpContext (@Nullable final HttpHost aProxy)
  {
    return createHttpContext (aProxy, (Credentials) null);
  }

  @Nonnull
  public static HttpContext createHttpContext (@Nullable final HttpHost aProxy,
                                               @Nullable final Credentials aProxyCredentials)
  {
    final HttpClientContext ret = HttpClientContext.create ();
    if (aProxy != null)
    {
      ret.setRequestConfig (RequestConfig.custom ().setProxy (aProxy).build ());
      if (aProxyCredentials != null)
      {
        final CredentialsProvider aCredentialsProvider = new BasicCredentialsProvider ();
        aCredentialsProvider.setCredentials (new AuthScope (aProxy), aProxyCredentials);
        ret.setCredentialsProvider (aCredentialsProvider);
      }
    }
    return ret;
  }
}
