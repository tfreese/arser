package de.freese.arser.repository;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Schaltet das type-Attribut komplett ab.
 *
 * @author Thomas Freese
 * @since 16.08.26
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public interface XmlTypeIgnoreMixIn {
}
