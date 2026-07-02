package de.freese.arser.api;

import java.io.InputStream;

import de.freese.arser.component.Lifecycle;

/**
 * @author Thomas Freese
 * @since 02.07.26
 */
public interface Arser extends Lifecycle {
    <R> ArserResult<R> download(String repositoryName, ArserRequest arserRequest);

    <R> ArserResult<R> exist(String repositoryName, ArserRequest arserRequest);

    <R> ArserResult<R> upload(String repositoryName, ArserRequest arserRequest, InputStream inputStream);
}
