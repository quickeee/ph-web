/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.web;

import java.nio.charset.Charset;

import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.charset.CCharset;
import com.helger.web.port.DefaultNetworkPorts;

/**
 * Contains some global web constants
 *
 * @author Philip Helger
 */
@Immutable
public final class CWeb
{
  /** Default FTP port */
  public static final int DEFAULT_PORT_FTP = DefaultNetworkPorts.TCP_21_ftp.getPort ();
  /** Default HTTP port */
  public static final int DEFAULT_PORT_HTTP = DefaultNetworkPorts.TCP_80_http.getPort ();
  /** Default HTTPS port */
  public static final int DEFAULT_PORT_HTTPS = DefaultNetworkPorts.TCP_443_https.getPort ();

  /** The scheme for HTTP */
  public static final String SCHEME_HTTP = "http";
  /** The scheme for HTTPS */
  public static final String SCHEME_HTTPS = "https";

  /** Default charset for requests: UTF-8 */
  public static final String CHARSET_REQUEST = CCharset.CHARSET_UTF_8;
  /** Default charset for requests: UTF-8 */
  public static final Charset CHARSET_REQUEST_OBJ = CCharset.CHARSET_UTF_8_OBJ;
  /** Default charset for multipart requests: UTF-8 */
  public static final String CHARSET_MULTIPART = CCharset.CHARSET_UTF_8;
  /** Default charset for multipart requests: UTF-8 */
  public static final Charset CHARSET_MULTIPART_OBJ = CCharset.CHARSET_UTF_8_OBJ;

  @PresentForCodeCoverage
  private static final CWeb s_aInstance = new CWeb ();

  private CWeb ()
  {}
}