// Created: 14.01.24
package de.freese.arser.jre.repository.builder;

import java.net.URI;

import de.freese.arser.blobstore.file.FileBlobStore;
import de.freese.arser.blobstore.jdbc.JdbcBlobStore;
import de.freese.arser.config.StoreConfig;
import de.freese.arser.core.component.BlobStoreComponent;
import de.freese.arser.core.component.DatasourceComponent;
import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.builder.AbstractRepositoryBuilder;
import de.freese.arser.core.repository.cached.CachedRepository;
import de.freese.arser.jre.repository.remote.JreHttpClientRemoteRepository;

/**
 * @author Thomas Freese
 */
public class RemoteRepositoryBuilder extends AbstractRepositoryBuilder<RemoteRepositoryBuilder> {
    private StoreConfig storeConfig;

    public Repository build(final LifecycleManager lifecycleManager, final JreHttpClientComponent httpClientComponent) {
        validateName();
        validateUri();

        Repository repository = new JreHttpClientRemoteRepository(getName(), getUri(), httpClientComponent::getHttpClient);
        lifecycleManager.add(repository);

        if (storeConfig != null) {
            if ("file".equalsIgnoreCase(storeConfig.getType())) {
                final URI uriCached = URI.create(storeConfig.getUri());

                final BlobStoreComponent blobStoreComponent = new BlobStoreComponent(new FileBlobStore(uriCached));
                lifecycleManager.add(blobStoreComponent);

                repository = new CachedRepository(repository, blobStoreComponent.getBlobStore());
                lifecycleManager.add(repository);
            }
            else if ("jdbc".equalsIgnoreCase(storeConfig.getType())) {
                final DatasourceComponent datasourceComponent = new DatasourceComponent(storeConfig, getName());
                lifecycleManager.add(datasourceComponent);

                final BlobStoreComponent blobStoreComponent = new BlobStoreComponent(new JdbcBlobStore(datasourceComponent::getDataSource));
                lifecycleManager.add(blobStoreComponent);

                repository = new CachedRepository(repository, blobStoreComponent.getBlobStore());
                lifecycleManager.add(repository);
            }
        }

        return repository;
    }

    public RemoteRepositoryBuilder storeConfig(final StoreConfig storeConfig) {
        this.storeConfig = storeConfig;

        return this;
    }

    @Override
    protected void validateUri() {
        super.validateUri();

        if (!"http".equalsIgnoreCase(getUri().getScheme()) && !"https".equalsIgnoreCase(getUri().getScheme())) {
            final String message = String.format("Ignoring RemoteRepository '%s', http/https URI scheme expected: %s", getName(), getUri());
            throw new IllegalStateException(message);
        }
    }
}
