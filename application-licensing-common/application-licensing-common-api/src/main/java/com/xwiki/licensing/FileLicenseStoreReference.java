package com.xwiki.licensing;

import java.io.File;

/**
 * Reference to a license store on the filesystem.
 *
 * @version $Id$
 */
public class FileLicenseStoreReference implements LicenseStoreReference
{
    private final File file;
    private final boolean isMulti;

    /**
     * Wrap a file or folder as a store reference.
     *
     * @param file a file or folder.
     */
    public FileLicenseStoreReference(File file)
    {
        this.file = file;
        this.isMulti = file.isDirectory();
    }

    /**
     * Wrap a file or folder as a store reference.
     *
     * @param file a file or folder (that may not exists).
     * @param isMulti true to force a multi key store when the file does not exists yet.
     *                Actually, Multi key store are always directory.
     */
    public FileLicenseStoreReference(File file, boolean isMulti)
    {
        this.file = file;
        this.isMulti = file.exists() ? file.isDirectory() : isMulti;
    }

    /**
     * @return the wrapped file reference.
     */
    public File getFile()
    {
        return this.file;
    }

    /**
     * @return true if this store is a directory.
     */
    public boolean isMulti()
    {
        return this.isMulti;
    }
}
