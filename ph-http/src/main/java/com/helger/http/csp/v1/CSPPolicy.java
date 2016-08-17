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
package com.helger.http.csp.v1;

import javax.annotation.concurrent.NotThreadSafe;

import com.helger.http.csp.AbstractCSPPolicy;

/**
 * CSP 1.0 policy. It's a list of {@link CSPDirective}.<br>
 * See http://www.w3.org/TR/CSP/
 *
 * @author Philip Helger
 * @since 6.0.3
 */
@NotThreadSafe
public class CSPPolicy extends AbstractCSPPolicy <CSPDirective>
{
  public CSPPolicy ()
  {}
}
