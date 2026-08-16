// package de.freese.arser.api;
//
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import de.freese.arser.component.LifeCycleRegistry;
// import de.freese.arser.repository.Repository;
// import de.freese.arser.repository.RepositoryBuilder;
// import de.freese.arser.repository.virtual.VirtualRepositoryBuilder;
// import de.freese.arser.utils.AbstractBuilder;
//
// /**
//  * @author Thomas Freese
//  * @since 04.07.26
//  */
// public final class ArserBuilder extends AbstractBuilder<ArserBuilder, Arser> {
//     public static ArserBuilder create() {
//         return new ArserBuilder();
//     }
//
//     private final List<RepositoryBuilder<?, ?>> repositoryBuilders = new ArrayList<>();
//     private final List<VirtualRepositoryBuilder> virtualRepositoryBuilders = new ArrayList<>();
//
//     ArserBuilder() {
//         super();
//     }
//
//     public ArserBuilder add(final RepositoryBuilder<?, ?> repositoryBuilder) {
//         repositoryBuilders.add(repositoryBuilder);
//
//         return self();
//     }
//
//     public ArserBuilder add(final VirtualRepositoryBuilder virtualRepositoryBuilder) {
//         virtualRepositoryBuilders.add(virtualRepositoryBuilder);
//
//         return self();
//     }
//
//     @Override
//     public Arser build(final LifeCycleRegistry lifeCycleRegistry) throws Exception {
//         if (repositoryBuilders.isEmpty()) {
//             throw new IllegalStateException("No repository builders defined");
//         }
//
//         final Map<String, Repository> repositoryMap = new HashMap<>();
//
//         for (final RepositoryBuilder<?, ?> repositoryBuilder : repositoryBuilders) {
//             final Repository repository = repositoryBuilder.build(lifeCycleRegistry);
//
//             if (repositoryMap.containsKey(repository.getName())) {
//                 throw new IllegalStateException("Repository already exists: " + repository.getName());
//             }
//
//             repositoryMap.put(repository.getName(), repository);
//         }
//
//         // VirtualRepositories
//         for (final VirtualRepositoryBuilder virtualRepositoryBuilder : virtualRepositoryBuilders) {
//             virtualRepositoryBuilder.repositoryProvider(repositoryMap::get);
//
//             if (repositoryMap.containsKey(virtualRepositoryBuilder.getName())) {
//                 throw new IllegalStateException("Repository already exists: " + virtualRepositoryBuilder.getName());
//             }
//
//             final Repository repository = virtualRepositoryBuilder.build(lifeCycleRegistry);
//             repositoryMap.put(repository.getName(), repository);
//         }
//
//         return new DefaultArser(repositoryMap);
//     }
//
//     @Override
//     protected ArserBuilder self() {
//         return this;
//     }
// }
