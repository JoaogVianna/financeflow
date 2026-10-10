package com.financeflow.service;

import com.financeflow.dto.LoginRequest;
import com.financeflow.dto.LoginResponse;
import com.financeflow.dto.RegisterRequest;
import com.financeflow.dto.UserResponse;
import com.financeflow.entity.User;
import com.financeflow.exception.ConflictException;
import com.financeflow.exception.UnauthorizedException;
import com.financeflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UserResponse register(RegisterRequest request) {
        // E-mail em minúsculas, pra "Ana@x.com" e "ana@x.com" serem a mesma conta.
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe uma conta com esse e-mail");
        }
        String hash = passwordEncoder.encode(request.password());
        User saved = userRepository.save(new User(request.name().trim(), email, hash));
        return UserResponse.from(saved);
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // Mesma mensagem pra "e-mail não existe" e "senha errada": quem tenta invadir
        // não descobre quais e-mails têm conta.
        User user = userRepository.findByEmail(email)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha inválidos"));

        return new LoginResponse(
                tokenService.generate(user),
                "Bearer",
                tokenService.expiresInSeconds(),
                UserResponse.from(user));
    }
}
