// The AI port and the rules that sit above it.
//
// Deliberately free of Spring and of any provider SDK: this module is the contract features
// are written against, and it has to stay callable from a plain unit test. Anything needing a
// client, a database, or configuration belongs in :api, behind these interfaces.
plugins {
    `java-library`
}
