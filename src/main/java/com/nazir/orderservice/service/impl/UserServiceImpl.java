package com.nazir.orderservice.service.impl;

import com.nazir.orderservice.dto.response.*;
import com.nazir.orderservice.entity.Address;
import com.nazir.orderservice.entity.Order;
import com.nazir.orderservice.entity.User;
import com.nazir.orderservice.exception.ResourceNotFoundException;
import com.nazir.orderservice.repository.AddressRepository;
import com.nazir.orderservice.repository.OrderRepository;
import com.nazir.orderservice.repository.UserRepository;
import com.nazir.orderservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository    userRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository   orderRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // PROFILE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = getUser(userId);
        List<Address> addresses = addressRepository.findByUserId(userId);
        return toResponse(user, addresses);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, String name, String phone) {
        User user = getUser(userId);
        if (name  != null && !name.isBlank())  user.setName(name);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        user = userRepository.save(user);
        List<Address> addresses = addressRepository.findByUserId(userId);
        log.info("Profile updated for userId={}", userId);
        return toResponse(user, addresses);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADDRESSES
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, String street, String city,
                                      String state, String country, String zipCode) {
        User user = getUser(userId);
        List<Address> existing = addressRepository.findByUserId(userId);

        Address address = Address.builder()
                .user(user)
                .street(street)
                .city(city)
                .state(state)
                .country(country)
                .zipCode(zipCode)
                .isDefault(existing.isEmpty()) // first address is default
                .build();

        address = addressRepository.save(address);
        log.info("Address added for userId={}", userId);
        return toAddressResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(UUID userId) {
        getUser(userId); // validate user exists
        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId,
                                         String street, String city,
                                         String state, String country, String zipCode) {
        Address address = getAddressForUser(userId, addressId);
        address.setStreet(street);
        address.setCity(city);
        address.setState(state);
        address.setCountry(country);
        address.setZipCode(zipCode);
        address = addressRepository.save(address);
        log.info("Address updated: addressId={}", addressId);
        return toAddressResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = getAddressForUser(userId, addressId);
        addressRepository.delete(address);

        // If deleted address was default, assign default to first remaining
        if (address.isDefault()) {
            addressRepository.findByUserId(userId).stream()
                    .findFirst()
                    .ifPresent(first -> {
                        first.setDefault(true);
                        addressRepository.save(first);
                    });
        }
        log.info("Address deleted: addressId={}", addressId);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        // Remove default from all addresses of user
        addressRepository.findByUserId(userId).forEach(a -> {
            a.setDefault(false);
            addressRepository.save(a);
        });

        // Set the chosen one as default
        Address address = getAddressForUser(userId, addressId);
        address.setDefault(true);
        address = addressRepository.save(address);
        log.info("Default address set: addressId={}", addressId);
        return toAddressResponse(address);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MY ORDERS (inside profile)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(UUID userId, int page, int size) {
        getUser(userId); // validate user exists
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return PageResponse.of(orders.map(this::toOrderSummary));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN OPERATIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(
                userRepository.findAll(pageable)
                        .map(u -> toResponse(u, addressRepository.findByUserId(u.getId())))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = getUser(id);
        return toResponse(user, addressRepository.findByUserId(id));
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = getUser(id);
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated: userId={}", id);
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        User user = getUser(id);
        user.setActive(true);
        userRepository.save(user);
        log.info("User activated: userId={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Address getAddressForUser(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        // Security: ensure address belongs to this user
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found");
        }
        return address;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────────

    private UserResponse toResponse(User user, List<Address> addresses) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.isActive())
                .addresses(addresses.stream().map(this::toAddressResponse).toList())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AddressResponse toAddressResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .street(a.getStreet())
                .city(a.getCity())
                .state(a.getState())
                .country(a.getCountry())
                .zipCode(a.getZipCode())
                .isDefault(a.isDefault())
                .build();
    }

    private OrderSummaryResponse toOrderSummary(Order o) {
        return OrderSummaryResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus().name())
                .totalAmount(o.getFinalAmount())
                .itemCount(o.getItems() != null ? o.getItems().size() : 0)
                .createdAt(o.getCreatedAt())
                .build();
    }
}