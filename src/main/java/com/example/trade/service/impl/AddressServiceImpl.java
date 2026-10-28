package com.example.trade.service.impl;

import com.example.trade.entity.Address;
import com.example.trade.entity.PageBean;
import com.example.trade.except.BusinessException;
import com.example.trade.mapper.AddressMapper;
import com.example.trade.service.AddressService;
import com.example.trade.utils.ThreadLocalUtil;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author maofan
 * @create 2026-10-27 20:20
 */
@Service
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressMapper addressMapper;

    @Override
    public void add(Address address) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        address.setUserId(id);
        addressMapper.add(address);
    }

    @Override
    public PageBean<Address> list() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        PageBean<Address> pageBean = new PageBean<>();
        List<Address> list = addressMapper.list(id);

        PageInfo<Address> pageInfo = new PageInfo<>(list);
        pageBean.setTotal((int) pageInfo.getTotal());
        pageBean.setItems(pageInfo.getList());
        return pageBean;
    }

    @Override
    public Address detail(Long id) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
      Address a =  addressMapper.getById(id);
      if (a==null){
          throw new BusinessException("地址不存在");
      }
        return addressMapper.detail(id,userId);
    }

    @Override
    public void update(Address address) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        address.setUserId(userId);
       addressMapper.update(address);
    }

    @Override
    public void delete(Long id) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        addressMapper.delete(id,userId);
    }

    @Override
    public void default1(Long id) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        addressMapper.clearDefault(userId);
        addressMapper.setDefault(id,userId);
    }
}
