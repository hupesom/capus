package com.example.trade.controller;

import com.example.trade.entity.Address;
import com.example.trade.entity.PageBean;
import com.example.trade.entity.Result;
import com.example.trade.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author maofan
 * @create 2026-10-27 20:19
 */
@RestController
@RequestMapping("address")
public class AddressController {
    @Autowired
    private AddressService addressService;
    @PatchMapping("default")
    public Result default1(Long id){
        addressService.default1(id);
        return Result.success();
    }

    @PostMapping
    public Result add(@RequestBody Address address) {
        addressService.add(address);
        return Result.success();
    }

    @GetMapping("list")
    public Result<PageBean<Address>> list() {
        PageBean<Address> addressPageBean = addressService.list();
        return Result.success(addressPageBean);
    }

    @GetMapping("detail")
    public Result detail(Long id) {
        Address address = addressService.detail(id);
        return Result.success(address);
    }

    @PutMapping("update")
    public Result update(@RequestBody Address address) {
        addressService.update(address);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(Long id) {
        addressService.delete(id);
        return Result.success();
    }


}
