package com.aileapro.domain;

/** What kind of knowledge this is, which decides how it is taught and how it ages. */
public enum KnowledgeClass {
    FOUNDATIONAL,
    STABLE,
    VERSION_SPECIFIC,
    CURRENT,
    EMERGING,
    EXPERIMENTAL,
    DEPRECATED
}
