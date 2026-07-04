package de.freese.arser.api;

import java.io.InputStream;
import java.util.Map;

import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 * @since 02.07.26
 */
public interface Arser {
    static ArserBuilder builder() {
        return new ArserBuilder();
    }

    <R> ArserResult<R> download(String repositoryName, ArserRequest arserRequest);

    <R> ArserResult<R> exist(String repositoryName, ArserRequest arserRequest);

    Map<String, Repository> getRepositories();

    <R> ArserResult<R> upload(String repositoryName, ArserRequest arserRequest, InputStream inputStream);
}
