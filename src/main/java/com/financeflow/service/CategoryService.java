package com.financeflow.service;

import com.financeflow.dto.CategoryRequest;
import com.financeflow.dto.CategoryResponse;
import com.financeflow.entity.Category;
import com.financeflow.entity.User;
import com.financeflow.exception.ConflictException;
import com.financeflow.exception.ResourceNotFoundException;
import com.financeflow.exception.UnauthorizedException;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository,
                           UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public CategoryResponse create(Long userId, CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("Você já tem uma categoria com o nome '" + name + "'");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));
        Category saved = categoryRepository.save(new Category(user, name, request.description()));
        return CategoryResponse.from(saved);
    }

    public List<CategoryResponse> findAll(Long userId) {
        return categoryRepository.findByUserId(userId, Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long userId, Long id) {
        return CategoryResponse.from(getCategory(userId, id));
    }

    public CategoryResponse update(Long userId, Long id, CategoryRequest request) {
        Category category = getCategory(userId, id);
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id)) {
            throw new ConflictException("Você já tem uma categoria com o nome '" + name + "'");
        }
        category.setName(name);
        category.setDescription(request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(Long userId, Long id) {
        Category category = getCategory(userId, id);
        if (transactionRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "A categoria '" + category.getName() + "' tem transações e não pode ser apagada");
        }
        categoryRepository.delete(category);
    }

    private Category getCategory(Long userId, Long id) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
    }
}
