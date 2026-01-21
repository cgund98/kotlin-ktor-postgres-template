package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.user.repo.JooqUserRepository
import com.github.cgund98.template.domain.user.repo.UserRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val userModule =
    module {
        // singleOf creates the instance and 'bind' tells Koin it's for the interface
        singleOf(::JooqUserRepository) { bind<UserRepository>() }

        // UserService can now be injected as normal
        singleOf(::UserService)
    }
