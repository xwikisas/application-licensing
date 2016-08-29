package com.xwiki.licensing.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.Converter;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseSerializer;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.SignedLicense;

/**
 * File system implementation of the license store.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("FileSystem")
public class FileSystemLicenseStore implements LicenseStore
{
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The "&gt;?xml " prefix in bytes.
     */
    private static final byte[] XML_MAGIC = new byte[] {0x3c, 0x3f, 0x78, 0x6d, 0x6c, 0x20};

    private static final String LICENSE_FILE_EXT = ".license";

    private static final FilenameFilter LICENSE_FILE_FILTER = new FilenameFilter()
    {
        private final Pattern pattern = Pattern.compile("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.license");

        @Override
        public boolean accept(File dir, String name)
        {
            return pattern.matcher(name).matches();
        }
    };

    @Inject
    @Named("xml")
    private LicenseSerializer serializer;

    @Inject
    private Converter<License> converter;

    private File getStoreFile(LicenseStoreReference store)
    {
        if (store instanceof FileLicenseStoreReference) {
            return ((FileLicenseStoreReference) store).getFile();
        }
        throw new IllegalArgumentException(String.format("Unsupported store reference [%s] for this implementation.",
            store.getClass().getName()));
    }

    private boolean isMulti(LicenseStoreReference store)
    {
        return !(store instanceof FileLicenseStoreReference) || ((FileLicenseStoreReference) store).isMulti();
    }

    private File getLicenseFile(LicenseStoreReference store, LicenseId licenseId)
    {
        File file = getStoreFile(store);

        if (!isMulti(store)) {
            throw new UnsupportedOperationException(
                String.format("Unexpected store reference, [%s] should be a multi-licenses store.", file));
        }

        return new File(file, licenseId.toString().toUpperCase() + LICENSE_FILE_EXT);
    }

    private Object getFileContent(File licenseFile) throws IOException
    {
        byte[] data = FileUtils.readFileToByteArray(licenseFile);

        boolean isXML = true;
        if (data.length > XML_MAGIC.length) {
            for (int i = 0; i < XML_MAGIC.length; i++) {
                if (data[i] != XML_MAGIC[i]) {
                    isXML = false;
                    break;
                }
            }
        }

        if (isXML) {
            return StringUtils.newStringUtf8(data);
        } else {
            return data;
        }
    }

    @Override
    public void store(LicenseStoreReference store, License license) throws IOException
    {
        File licenseFile = (isMulti(store)) ? getLicenseFile(store, license.getId()) : getStoreFile(store);

        if (license instanceof SignedLicense) {
            FileUtils.writeByteArrayToFile(licenseFile, ((SignedLicense) license).getEncoded());
        } else {
            FileUtils.writeStringToFile(licenseFile, (String) serializer.serialize(license), UTF8);
        }
    }

    @Override
    public License retrieve(LicenseStoreReference store) throws IOException
    {
        File licenseFile = getStoreFile(store);

        if (isMulti(store)) {
            throw new UnsupportedOperationException(
                String.format("Unexpected store reference, [%s] should be a single license store.", licenseFile));
        }

        if (!licenseFile.exists()) {
            return null;
        }

        return converter.convert(License.class, getFileContent(licenseFile));
    }

    @Override
    public License retrieve(LicenseStoreReference store, LicenseId licenseId) throws IOException
    {
        File licenseFile = getLicenseFile(store, licenseId);

        if (!licenseFile.exists()) {
            return null;
        }

        return converter.convert(License.class, getFileContent(licenseFile));
    }

    @Override
    public Iterable<License> getIterable(LicenseStoreReference store)
    {
        if (!isMulti(store)) {
            throw new UnsupportedOperationException(
                String.format("Unexpected store reference, cannot iterate on a single license store [%s].",
                    getStoreFile(store)));
        }

        return new Iterable<License>()
        {
            @Override
            public Iterator<License> iterator()
            {
                return new LicenseFileIterator(
                    getStoreFile(store).listFiles(LICENSE_FILE_FILTER)
                );
            }
        };
    }

    @Override
    public void delete(LicenseStoreReference store)
    {
        File licenseFile = getStoreFile(store);

        if (isMulti(store)) {
            try {
                FileUtils.deleteDirectory(licenseFile);
            } catch (IOException e) {
                // Ignored
            }
        } else {
            licenseFile.delete();
        }
    }

    @Override
    public void delete(LicenseStoreReference store, LicenseId licenseId)
    {
        File licenseFile = getLicenseFile(store, licenseId);

        if (licenseFile.exists()) {
            licenseFile.delete();
        }
    }

    class LicenseFileIterator implements Iterator<License>
    {
        private final File[] files;
        private int index;

        LicenseFileIterator(File[] files)
        {
            this.files = files;
        }

        public boolean hasNext()
        {
            return index < files.length;
        }

        public License next()
        {
            try {
                return converter.convert(License.class, getFileContent(files[index++]));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Removing licenses is not supported by this iterator.");
        }
    }
}
