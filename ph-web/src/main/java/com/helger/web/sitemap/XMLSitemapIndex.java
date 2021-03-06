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
package com.helger.web.sitemap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.ToStringGenerator;
import com.helger.datetime.util.PDTWebDateHelper;
import com.helger.servlet.StaticServerInfo;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.IXMLWriterSettings;

/**
 * Contains a set of {@link XMLSitemapURLSet} objects. Necessary to group
 * multiple sitemaps when the number of URLs or the total size of a single URL
 * set is exceeded.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class XMLSitemapIndex implements Serializable
{
  public static final boolean DEFAULT_USE_GZIP = true;
  private static final String ELEMENT_SITEMAPINDEX = "sitemapindex";
  private static final String ELEMENT_SITEMAP = "sitemap";
  private static final String ELEMENT_LOC = "loc";
  private static final String ELEMENT_LASTMOD = "lastmod";
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLSitemapIndex.class);

  private final ICommonsList <XMLSitemapURLSet> m_aURLSets = new CommonsArrayList <> ();
  private final boolean m_bUseGZip;

  /**
   * Constructor using GZip output by default
   */
  public XMLSitemapIndex ()
  {
    this (DEFAULT_USE_GZIP);
  }

  /**
   * Constructor
   *
   * @param bUseGZip
   *        If <code>true</code> all contained URL sets are written to disk
   *        using the GZip algorithm
   */
  public XMLSitemapIndex (final boolean bUseGZip)
  {
    m_bUseGZip = bUseGZip;
  }

  public boolean isUseGZip ()
  {
    return m_bUseGZip;
  }

  @Nonnegative
  public int getURLSetCount ()
  {
    return m_aURLSets.size ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <XMLSitemapURLSet> getAllURLSets ()
  {
    return m_aURLSets.getClone ();
  }

  public void addURLSet (@Nonnull final XMLSitemapURLSet aURLSet)
  {
    ValueEnforcer.notNull (aURLSet, "URLset");

    if (aURLSet.isMultiFileSitemap ())
    {
      // Split into several smaller URL sets
      final int nEntries = aURLSet.getURLCount ();
      XMLSitemapURLSet aNewURLSet = new XMLSitemapURLSet ();
      for (int i = 0; i < nEntries; ++i)
      {
        final XMLSitemapURL aCurrentURL = aURLSet.getURL (i);
        aNewURLSet.addURL (aCurrentURL);
        if (aNewURLSet.isMultiFileSitemap ())
        {
          // We do have an overflow -> remove the last item and add the URL set
          aNewURLSet.removeLastURL ();
          m_aURLSets.add (aNewURLSet);

          // start with a new URL set containing the last overflow-creating
          // entry
          aNewURLSet = new XMLSitemapURLSet ();
          aNewURLSet.addURL (aCurrentURL);
        }
      }

      // Append last URL - always contains something!
      m_aURLSets.add (aNewURLSet);
    }
    else
      m_aURLSets.add (aURLSet);
  }

  @Nonnull
  @Nonempty
  public static String getSitemapFilename (@Nonnegative final int nIndex, final boolean bUseGZip)
  {
    return "sitemap" + nIndex + ".xml" + (bUseGZip ? ".gz" : "");
  }

  /**
   * Get the name of the sitemap file at the specified index
   *
   * @param nIndex
   *        The index to be used. Should be ge; 0.
   * @return The name of the sitemap file. Neither <code>null</code> nor empty.
   * @see #getSitemapFilename(int, boolean)
   */
  @Nonnull
  @Nonempty
  public String getSitemapFilename (@Nonnegative final int nIndex)
  {
    return getSitemapFilename (nIndex, m_bUseGZip);
  }

  @Nonnull
  public IMicroDocument getAsDocument ()
  {
    final String sNamespaceURL = CXMLSitemap.XML_NAMESPACE_0_9;
    final IMicroDocument ret = new MicroDocument ();
    final IMicroElement eSitemapindex = ret.appendElement (sNamespaceURL, ELEMENT_SITEMAPINDEX);
    int nIndex = 0;
    for (final XMLSitemapURLSet aURLSet : m_aURLSets)
    {
      final IMicroElement eSitemap = eSitemapindex.appendElement (sNamespaceURL, ELEMENT_SITEMAP);

      // The location of the sub-sitemaps must be prefixed with the full server
      // and context path
      eSitemap.appendElement (sNamespaceURL, ELEMENT_LOC)
              .appendText (StaticServerInfo.getInstance ().getFullContextPath () + "/" + getSitemapFilename (nIndex));

      final LocalDateTime aLastModification = aURLSet.getLastModificationDateTime ();
      if (aLastModification != null)
        eSitemap.appendElement (sNamespaceURL, ELEMENT_LASTMOD)
                .appendText (PDTWebDateHelper.getAsStringXSD (aLastModification));
      ++nIndex;
    }
    return ret;
  }

  @Nonnull
  protected IXMLWriterSettings getXMLWriterSettings ()
  {
    // Important: No indent and align, because otherwise the calculated output
    // length would not be suitable
    return CXMLSitemap.XML_WRITER_SETTINGS;
  }

  @Nonnull
  public String getAsXMLString ()
  {
    return MicroWriter.getNodeAsString (getAsDocument (), getXMLWriterSettings ());
  }

  @Nonnull
  private OutputStream _createOutputStream (@Nonnull final File aFile)
  {
    OutputStream aOS = FileHelper.getOutputStream (aFile);
    if (m_bUseGZip)
      try
      {
        aOS = new GZIPOutputStream (aOS);
      }
      catch (final IOException ex)
      {
        throw new IllegalStateException ("Failed to create GZip OutputStream for " + aFile, ex);
      }
    return aOS;
  }

  @Nonnull
  public ESuccess writeToDisk (@Nonnull final File aBaseDir)
  {
    ValueEnforcer.notNull (aBaseDir, "Basedir");
    if (!FileHelper.existsDir (aBaseDir))
      throw new IllegalArgumentException ("The passed directory does not exist: " + aBaseDir);

    if (m_aURLSets.isEmpty ())
    {
      s_aLogger.warn ("No URL sets contained - not doing anything!");
      return ESuccess.FAILURE;
    }

    // Write base file
    if (MicroWriter.writeToFile (getAsDocument (),
                                 new File (aBaseDir, CXMLSitemap.SITEMAP_ENTRY_FILENAME),
                                 getXMLWriterSettings ())
                   .isFailure ())
    {
      s_aLogger.error ("Failed to write " + CXMLSitemap.SITEMAP_ENTRY_FILENAME + " file!");
      return ESuccess.FAILURE;
    }

    // Write all URL sets
    int nIndex = 0;
    for (final XMLSitemapURLSet aURLSet : m_aURLSets)
    {
      final String sFilename = getSitemapFilename (nIndex);
      final File aFile = new File (aBaseDir, sFilename);
      final OutputStream aOS = _createOutputStream (aFile);
      if (MicroWriter.writeToStream (aURLSet.getAsDocument (), aOS, getXMLWriterSettings ()).isFailure ())
      {
        s_aLogger.error ("Failed to write single sitemap file " + aFile);
        return ESuccess.FAILURE;
      }
      nIndex++;
    }

    return ESuccess.SUCCESS;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final XMLSitemapIndex rhs = (XMLSitemapIndex) o;
    return m_aURLSets.equals (rhs.m_aURLSets) && m_bUseGZip == rhs.m_bUseGZip;
  }

  @Override
  public int hashCode ()
  {
    // Don't compare the other fields as they are calculated
    return new HashCodeGenerator (this).append (m_aURLSets).append (m_bUseGZip).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("URLSets", m_aURLSets).append ("useGZip", m_bUseGZip).toString ();
  }
}
