package com.financeflow.service;

import com.financeflow.dto.CategoryRequest;
import com.financeflow.dto.CategoryResponse;
import com.financeflow.entity.Category;
import com.financeflow.exception.ConflictException;
import com.financeflow.exception.ResourceNotFoundException;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.TransactionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Já existe uma categoria com o nome '" + name + "'");
        }
        Category saved = categoryRepository.save(new Category(name, request.description()));
        return CategoryResponse.from(saved);
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getCategory(id));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Já existe uma categoria com o nome '" + name + "'");
        }
        category.setName(name);
        category.setDescription(request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(Long id) {
        Category category = getCategory(id);
        if (transactionRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "A categoria '" + category.getName() + "' tem transações e não pode ser apagada");
        }
        categoryRepository.delete(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
    }
}
