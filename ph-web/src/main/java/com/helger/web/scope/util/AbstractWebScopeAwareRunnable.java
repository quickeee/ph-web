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
package com.helger.web.scope.util;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import com.helger.commons.annotation.Nonempty;
import com.helger.servlet.mock.MockHttpServletResponse;
import com.helger.servlet.mock.OfflineHttpServletRequest;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Abstract implementation of {@link Runnable} that handles WebScopes correctly.
 *
 * @author Philip Helger
 */
public abstract class AbstractWebScopeAwareRunnable extends AbstractWebScopeAwareAction implements Runnable
{
  public AbstractWebScopeAwareRunnable ()
  {
    this (WebScopeManager.getGlobalScope ().getServletContext (), WebScopeManager.getApplicationScope ().getID ());
  }

  public AbstractWebScopeAwareRunnable (@Nonnull final ServletContext aSC,
                                        @Nonnull @Nonempty final String sApplicationID)
  {
    super (aSC, sApplicationID);
  }

  /**
   * Implement your code in here.
   */
  protected abstract void scopedRun ();

  public final void run ()
  {
    WebScopeManager.onRequestBegin (m_sApplicationID,
                                    new OfflineHttpServletRequest (m_aSC, false),
                                    new MockHttpServletResponse ());
    try
    {
      scopedRun ();
    }
    finally
    {
      WebScopeManager.onRequestEnd ();
    }
  }
}
