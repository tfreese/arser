// Created: 14.01.24
package de.freese.arser.core.repository.builder;

import de.freese.arser.blobstore.file.FileBlobStore;
import de.freese.arser.core.component.BlobStoreComponent;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.blobstore.BlobStoreRepository;
import de.freese.arser.core.repository.configurer.LocalRepositoryConfigurer;
import de.freese.arser.core.repository.local.FileRepository;

/**
 * @author Thomas Freese
 */
public class LocalRepositoryBuilder extends AbstractRepositoryBuilder implements LocalRepositoryConfigurer {
    private boolean writeable;

    public Repository build(final LifecycleManager lifecycleManager) {
        validateName();
        validateUri();

        final Repository repository;

        if (writeable) {
            final BlobStoreComponent blobStoreComponent = new BlobStoreComponent(new FileBlobStore(getUri()));
            lifecycleManager.add(blobStoreComponent);

            repository = new BlobStoreRepository(getName(), getUri(), blobStoreComponent.getBlobStore());
        }
        else {
            repository = new FileRepository(getName(), getUri());
        }

        lifecycleManager.add(repository);

        return repository;
    }

    @Override
    public void setWriteable(final boolean writeable) {
        this.writeable = writeable;
    }

    @Override
    protected void validateUri() {
        super.validateUri();

        if (!"file".equalsIgnoreCase(getUri().getScheme())) {
            final String message = String.format("Ignoring LocalRepository '%s', file URI scheme expected: %s", getName(), getUri());
            throw new IllegalStateException(message);
        }
    }
}
