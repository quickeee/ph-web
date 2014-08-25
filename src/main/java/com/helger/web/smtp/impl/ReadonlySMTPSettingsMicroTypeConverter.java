/**
 * Copyright (C) 2006-2014 phloc systems (www.phloc.com)
 * Copyright (C) 2014 Philip Helger (www.helger.com)
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
package com.helger.web.smtp.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotations.ContainsSoftMigration;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.convert.IMicroTypeConverter;
import com.helger.web.smtp.ISMTPSettings;

public final class ReadonlySMTPSettingsMicroTypeConverter implements IMicroTypeConverter
{
  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aSource,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final ISMTPSettings aSMTPSettings = (ISMTPSettings) aSource;
    return SMTPSettingsMicroTypeConverter.convertToMicroElement (aSMTPSettings, sNamespaceURI, sTagName);
  }

  @Nonnull
  @ContainsSoftMigration
  public ReadonlySMTPSettings convertToNative (@Nonnull final IMicroElement eSMTPSettings)
  {
    final SMTPSettings aSettings = SMTPSettingsMicroTypeConverter.convertToSMTPSettings (eSMTPSettings);
    return new ReadonlySMTPSettings (aSettings);
  }
}
