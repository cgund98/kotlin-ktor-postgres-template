package com.github.cgund98.template.presentation.user

import com.github.cgund98.template.domain.user.UserService
import com.github.cgund98.template.domain.user.repo.ExposedUserRepository
import com.github.cgund98.template.domain.user.repo.UserRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val userModule =
    module {
        // singleOf creates the instance and 'bind' tells Koin it's for the interface
        singleOf(::ExposedUserRepository) { bind<UserRepository>() }

        // UserService can now be injected as normal
        singleOf(::UserService)
    }
