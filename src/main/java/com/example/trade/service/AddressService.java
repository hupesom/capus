package com.example.trade.service;

import com.example.trade.entity.Address;
import com.example.trade.entity.PageBean;

/**
 * @author maofan
 * @create 2026-10-27 20:19
 */
public interface AddressService {
    void add(Address address);

    PageBean<Address> list();

    Address detail(Long id);

    void update(Address address);

    void delete(Long id);

    void default1(Long id);
}
