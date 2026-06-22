// Created: 19.07.23
package de.freese.arser.core.repository;

import de.freese.arser.core.component.Lifecycle;
import de.freese.arser.core.model.ArserRequest;
import de.freese.arser.core.model.ArserResult;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    boolean exist(ArserRequest arserRequest) throws Exception;

    String getName();

    <R> ArserResult<R> getResource(ArserRequest arserRequest) throws Exception;
}
