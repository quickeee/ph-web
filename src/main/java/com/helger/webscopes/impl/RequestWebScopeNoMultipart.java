/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.webscopes.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.annotations.OverrideOnDemand;
import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.commons.collections.ArrayHelper;
import com.helger.commons.collections.ContainerHelper;
import com.helger.commons.equals.EqualsUtils;
import com.helger.commons.idfactory.GlobalIDFactory;
import com.helger.commons.lang.CGStringHelper;
import com.helger.commons.scopes.AbstractMapBasedScope;
import com.helger.commons.scopes.ScopeUtils;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.web.fileupload.IFileItem;
import com.helger.web.http.EHTTPMethod;
import com.helger.web.http.EHTTPVersion;
import com.helger.web.servlet.request.IRequestParamMap;
import com.helger.web.servlet.request.RequestHelper;
import com.helger.web.servlet.request.RequestParamMap;
import com.helger.webscopes.domain.IRequestWebScope;

/**
 * A request web scopes that does not parse multipart requests.
 *
 * @author Philip Helger
 */
public class RequestWebScopeNoMultipart extends AbstractMapBasedScope implements IRequestWebScope
{
  // Because of transient field
  private static final long serialVersionUID = 78563987233146L;

  private static final Logger s_aLogger = LoggerFactory.getLogger (RequestWebScopeNoMultipart.class);
  private static final String REQUEST_ATTR_SCOPE_INITED = "$ph.request.scope.inited";
  private static final String REQUEST_ATTR_REQUESTPARAMMAP = "$ph.request.scope.requestparammap";

  protected final transient HttpServletRequest m_aHttpRequest;
  protected final transient HttpServletResponse m_aHttpResponse;

  @Nonnull
  @Nonempty
  private static String _createScopeID (@Nonnull final HttpServletRequest aHttpRequest)
  {
    ValueEnforcer.notNull (aHttpRequest, "HttpRequest");

    return GlobalIDFactory.getNewIntID () + "@" + aHttpRequest.getRequestURI ();
  }

  public RequestWebScopeNoMultipart (@Nonnull final HttpServletRequest aHttpRequest,
                                     @Nonnull final HttpServletResponse aHttpResponse)
  {
    super (_createScopeID (aHttpRequest));

    m_aHttpRequest = aHttpRequest;
    m_aHttpResponse = ValueEnforcer.notNull (aHttpResponse, "HttpResponse");

    // done initialization
    if (ScopeUtils.debugRequestScopeLifeCycle (s_aLogger))
      s_aLogger.info ("Created request web scope '" +
                          getID () +
                          "' of class " +
                          CGStringHelper.getClassLocalName (this),
                      ScopeUtils.getDebugStackTrace ());
  }

  @OverrideOnDemand
  protected boolean addSpecialRequestAttributes ()
  {
    return false;
  }

  @OverrideOnDemand
  protected void postAttributeInit ()
  {}

  public final void initScope ()
  {
    // Avoid double initialization of a scope, because for file uploads, the
    // parameters can only be extracted once!
    // As the parameters are stored directly in the HTTP request, we're not
    // loosing any data here!
    if (getAndSetAttributeFlag (REQUEST_ATTR_SCOPE_INITED))
    {
      s_aLogger.warn ("Scope was already inited: " + toString ());
      return;
    }

    // where some extra items (like file items) handled?
    final boolean bAddedSpecialRequestAttrs = addSpecialRequestAttributes ();

    // set parameters as attributes (handles GET and POST parameters)
    final Enumeration <?> aEnum = m_aHttpRequest.getParameterNames ();
    while (aEnum.hasMoreElements ())
    {
      final String sParamName = (String) aEnum.nextElement ();

      // Avoid double setting a parameter!
      if (bAddedSpecialRequestAttrs && containsAttribute (sParamName))
        continue;

      // Check if it is a single value or not
      final String [] aParamValues = m_aHttpRequest.getParameterValues (sParamName);
      if (aParamValues.length == 1)
        setAttribute (sParamName, aParamValues[0]);
      else
        setAttribute (sParamName, aParamValues);
    }

    postAttributeInit ();

    // done initialization
    if (ScopeUtils.debugRequestScopeLifeCycle (s_aLogger))
      s_aLogger.info ("Initialized request web scope '" +
                          getID () +
                          "' of class " +
                          CGStringHelper.getClassLocalName (this),
                      ScopeUtils.getDebugStackTrace ());
  }

  @Override
  protected void postDestroy ()
  {
    if (ScopeUtils.debugRequestScopeLifeCycle (s_aLogger))
      s_aLogger.info ("Destroyed request web scope '" +
                          getID () +
                          "' of class " +
                          CGStringHelper.getClassLocalName (this),
                      ScopeUtils.getDebugStackTrace ());
  }

  @Nonnull
  @Nonempty
  public final String getSessionID ()
  {
    return getSessionID (true);
  }

  @Nullable
  public final String getSessionID (final boolean bCreateIfNotExisting)
  {
    final HttpSession aSession = m_aHttpRequest.getSession (bCreateIfNotExisting);
    return aSession == null ? null : aSession.getId ();
  }

  @Nullable
  public List <String> getAttributeValues (@Nullable final String sName)
  {
    return getAttributeValues (sName, null);
  }

  @Nullable
  public List <String> getAttributeValues (@Nullable final String sName, @Nullable final List <String> aDefault)
  {
    final Object aValue = getAttributeObject (sName);
    if (aValue instanceof String [])
    {
      // multiple values passed in the request
      return ContainerHelper.newList ((String []) aValue);
    }
    if (aValue instanceof String)
    {
      // single value passed in the request
      return ContainerHelper.newList ((String) aValue);
    }
    // E.g. for file items
    return aDefault;
  }

  public boolean hasAttributeValue (@Nullable final String sName, @Nullable final String sDesiredValue)
  {
    return EqualsUtils.equals (getAttributeAsString (sName), sDesiredValue);
  }

  public boolean hasAttributeValue (@Nullable final String sName,
                                    @Nullable final String sDesiredValue,
                                    final boolean bDefault)
  {
    final String sValue = getAttributeAsString (sName);
    return sValue == null ? bDefault : EqualsUtils.equals (sValue, sDesiredValue);
  }

  /**
   * Returns the name of the character encoding used in the body of this
   * request. This method returns <code>null</code> if the request does not
   * specify a character encoding
   *
   * @return a <code>String</code> containing the name of the character
   *         encoding, or <code>null</code> if the request does not specify a
   *         character encoding
   */
  @Nullable
  public String getCharacterEncoding ()
  {
    return m_aHttpRequest.getCharacterEncoding ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, IFileItem> getAllUploadedFileItems ()
  {
    final Map <String, IFileItem> ret = new HashMap <String, IFileItem> ();
    final Enumeration <String> aEnum = getAttributeNames ();
    while (aEnum.hasMoreElements ())
    {
      final String sAttrName = aEnum.nextElement ();
      final Object aAttrValue = getAttributeObject (sAttrName);
      if (aAttrValue instanceof IFileItem)
        ret.put (sAttrName, (IFileItem) aAttrValue);
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, IFileItem []> getAllUploadedFileItemsComplete ()
  {
    final Map <String, IFileItem []> ret = new HashMap <String, IFileItem []> ();
    final Enumeration <String> aEnum = getAttributeNames ();
    while (aEnum.hasMoreElements ())
    {
      final String sAttrName = aEnum.nextElement ();
      final Object aAttrValue = getAttributeObject (sAttrName);
      if (aAttrValue instanceof IFileItem)
        ret.put (sAttrName, new IFileItem [] { (IFileItem) aAttrValue });
      else
        if (aAttrValue instanceof IFileItem [])
          ret.put (sAttrName, ArrayHelper.getCopy ((IFileItem []) aAttrValue));
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <IFileItem> getAllUploadedFileItemValues ()
  {
    final List <IFileItem> ret = new ArrayList <IFileItem> ();
    final Enumeration <String> aEnum = getAttributeNames ();
    while (aEnum.hasMoreElements ())
    {
      final String sAttrName = aEnum.nextElement ();
      final Object aAttrValue = getAttributeObject (sAttrName);
      if (aAttrValue instanceof IFileItem)
        ret.add ((IFileItem) aAttrValue);
      else
        if (aAttrValue instanceof IFileItem [])
          for (final IFileItem aChild : (IFileItem []) aAttrValue)
            ret.add (aChild);
    }
    return ret;
  }

  @Nullable
  public IFileItem getAttributeAsFileItem (@Nullable final String sAttrName)
  {
    final Object aObject = getAttributeObject (sAttrName);
    return aObject instanceof IFileItem ? (IFileItem) aObject : null;
  }

  @Nonnull
  public IRequestParamMap getRequestParamMap ()
  {
    // Check if a value is cached in the scope
    IRequestParamMap aValue = getCastedAttribute (REQUEST_ATTR_REQUESTPARAMMAP);
    if (aValue == null)
    {
      // Use all attributes except the internal ones
      final Map <String, Object> aAttrs = getAllAttributes ();
      aAttrs.remove (REQUEST_ATTR_SCOPE_INITED);
      // Request the map and put it in scope
      aValue = RequestParamMap.create (aAttrs);
      setAttribute (REQUEST_ATTR_REQUESTPARAMMAP, aValue);
    }
    return aValue;
  }

  public String getScheme ()
  {
    return m_aHttpRequest.getScheme ();
  }

  public String getServerName ()
  {
    return m_aHttpRequest.getServerName ();
  }

  public String getProtocol ()
  {
    return m_aHttpRequest.getProtocol ();
  }

  @Nullable
  public EHTTPVersion getHttpVersion ()
  {
    return RequestHelper.getHttpVersion (m_aHttpRequest);
  }

  public int getServerPort ()
  {
    return m_aHttpRequest.getServerPort ();
  }

  public String getMethod ()
  {
    return m_aHttpRequest.getMethod ();
  }

  @Nullable
  public EHTTPMethod getHttpMethod ()
  {
    return RequestHelper.getHttpMethod (m_aHttpRequest);
  }

  @Nullable
  public String getPathInfo ()
  {
    return RequestHelper.getPathInfo (m_aHttpRequest);
  }

  @Nonnull
  public String getPathWithinServletContext ()
  {
    return RequestHelper.getPathWithinServletContext (m_aHttpRequest);
  }

  @Nonnull
  public String getPathWithinServlet ()
  {
    return RequestHelper.getPathWithinServlet (m_aHttpRequest);
  }

  public String getPathTranslated ()
  {
    return m_aHttpRequest.getPathTranslated ();
  }

  public String getQueryString ()
  {
    return m_aHttpRequest.getQueryString ();
  }

  public String getRemoteHost ()
  {
    return m_aHttpRequest.getRemoteHost ();
  }

  public String getRemoteAddr ()
  {
    return m_aHttpRequest.getRemoteAddr ();
  }

  public String getAuthType ()
  {
    return m_aHttpRequest.getAuthType ();
  }

  public String getRemoteUser ()
  {
    return m_aHttpRequest.getRemoteUser ();
  }

  public String getContentType ()
  {
    return m_aHttpRequest.getContentType ();
  }

  public long getContentLength ()
  {
    return RequestHelper.getContentLength (m_aHttpRequest);
  }

  @Nonnull
  public String getRequestURI ()
  {
    return RequestHelper.getRequestURI (m_aHttpRequest);
  }

  @Nonnull
  public String getServletPath ()
  {
    return m_aHttpRequest.getServletPath ();
  }

  public HttpSession getSession (final boolean bCreateIfNotExisting)
  {
    return m_aHttpRequest.getSession (bCreateIfNotExisting);
  }

  @Nonnull
  private StringBuilder _getFullServerPath ()
  {
    return RequestHelper.getFullServerName (m_aHttpRequest.getScheme (),
                                            m_aHttpRequest.getServerName (),
                                            m_aHttpRequest.getServerPort ());
  }

  @Nonnull
  public String getFullServerPath ()
  {
    return _getFullServerPath ().toString ();
  }

  @Nonnull
  public String getContextPath ()
  {
    // In some rare scenarios, Tomcat 7 may return null here!
    return StringHelper.getNotNull (m_aHttpRequest.getContextPath (), "");
  }

  @Nonnull
  public String getFullContextPath ()
  {
    return _getFullServerPath ().append (getContextPath ()).toString ();
  }

  /**
   * This is a heuristic method to determine whether a request is for a file
   * (e.g. x.jsp) or for a servlet. It is assumed that regular servlets don't
   * have a '.' in their name!
   *
   * @param sServletPath
   *        The non-<code>null</code> servlet path to check
   * @return <code>true</code> if it is assumed that the request is file based,
   *         <code>false</code> if it can be assumed to be a regular servlet.
   */
  public static boolean isFileBasedRequest (@Nonnull final String sServletPath)
  {
    return sServletPath.indexOf ('.') >= 0;
  }

  @Nonnull
  public String getContextAndServletPath ()
  {
    final String sServletPath = getServletPath ();
    if (isFileBasedRequest (sServletPath))
      return getContextPath () + sServletPath;
    // For servlets that are not files, we need to append a trailing slash
    return getContextPath () + sServletPath + '/';
  }

  @Nonnull
  public String getFullContextAndServletPath ()
  {
    final String sServletPath = getServletPath ();
    if (isFileBasedRequest (sServletPath))
      return getFullContextPath () + sServletPath;
    // For servlets, we need to append a trailing slash
    return getFullContextPath () + sServletPath + '/';
  }

  @Nonnull
  @Nonempty
  public String getURL ()
  {
    return RequestHelper.getURL (m_aHttpRequest);
  }

  @Nonnull
  public String encodeURL (@Nonnull final String sURL)
  {
    return m_aHttpResponse.encodeURL (sURL);
  }

  @Nonnull
  public ISimpleURL encodeURL (@Nonnull final ISimpleURL aURL)
  {
    ValueEnforcer.notNull (aURL, "URL");

    // Encode only the path and copy params and anchor
    return new SimpleURL (encodeURL (aURL.getPath ()), aURL.getAllParams (), aURL.getAnchor ());
  }

  @Nonnull
  public String encodeRedirectURL (@Nonnull final String sURL)
  {
    return m_aHttpResponse.encodeRedirectURL (sURL);
  }

  @Nonnull
  public ISimpleURL encodeRedirectURL (@Nonnull final ISimpleURL aURL)
  {
    ValueEnforcer.notNull (aURL, "URL");

    // Encode only the path and copy params and anchor
    return new SimpleURL (encodeRedirectURL (aURL.getPath ()), aURL.getAllParams (), aURL.getAnchor ());
  }

  public boolean areCookiesEnabled ()
  {
    // Just check whether the session ID is appended to the URL or not
    return "a".equals (encodeURL ("a"));
  }

  @Nullable
  public String getRequestHeader (@Nullable final String sName)
  {
    return m_aHttpRequest.getHeader (sName);
  }

  @Nullable
  public Enumeration <String> getRequestHeaders (@Nullable final String sName)
  {
    return RequestHelper.getRequestHeaders (m_aHttpRequest, sName);
  }

  @Nullable
  public Enumeration <String> getRequestHeaderNames ()
  {
    return RequestHelper.getRequestHeaderNames (m_aHttpRequest);
  }

  public Cookie [] getCookies ()
  {
    return m_aHttpRequest.getCookies ();
  }

  public boolean isUserInRole (final String sRole)
  {
    return m_aHttpRequest.isUserInRole (sRole);
  }

  @Nullable
  public Principal getUserPrincipal ()
  {
    return m_aHttpRequest.getUserPrincipal ();
  }

  @Nullable
  public String getRequestedSessionId ()
  {
    return m_aHttpRequest.getRequestedSessionId ();
  }

  public StringBuffer getRequestURL ()
  {
    return m_aHttpRequest.getRequestURL ();
  }

  public boolean isRequestedSessionIdValid ()
  {
    return m_aHttpRequest.isRequestedSessionIdValid ();
  }

  public boolean isRequestedSessionIdFromCookie ()
  {
    return m_aHttpRequest.isRequestedSessionIdFromCookie ();
  }

  public boolean isRequestedSessionIdFromURL ()
  {
    return m_aHttpRequest.isRequestedSessionIdFromURL ();
  }

  public int getRemotePort ()
  {
    return m_aHttpRequest.getRemotePort ();
  }

  public boolean isSecure ()
  {
    return m_aHttpRequest.isSecure ();
  }

  public String getLocalName ()
  {
    return m_aHttpRequest.getLocalName ();
  }

  public String getLocalAddr ()
  {
    return m_aHttpRequest.getLocalAddr ();
  }

  public int getLocalPort ()
  {
    return m_aHttpRequest.getLocalPort ();
  }

  @Nonnull
  public HttpServletRequest getRequest ()
  {
    return m_aHttpRequest;
  }

  @Nonnull
  public HttpServletResponse getResponse ()
  {
    return m_aHttpResponse;
  }

  @Nonnull
  public OutputStream getOutputStream () throws IOException
  {
    return m_aHttpResponse.getOutputStream ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("httpRequest", m_aHttpRequest)
                            .append ("httpResponse", m_aHttpResponse)
                            .toString ();
  }
}