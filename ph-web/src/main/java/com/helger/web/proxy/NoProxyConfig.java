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
package com.helger.web.proxy;

import java.net.Proxy;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.string.ToStringGenerator;

/**
 * Class for indicating direct Internet access (no proxy needed).
 *
 * @author Philip Helger
 */
@Immutable
public class NoProxyConfig implements IProxyConfig
{
  public NoProxyConfig ()
  {}

  public void activateGlobally ()
  {
    // Deactivate all other proxy configurations
    HttpProxyConfig.deactivateGlobally ();
    SocksProxyConfig.deactivateGlobally ();
    UseSystemProxyConfig.deactivateGlobally ();
  }

  @Nonnull
  public Proxy getAsProxy ()
  {
    return Proxy.NO_PROXY;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).toString ();
  }
}