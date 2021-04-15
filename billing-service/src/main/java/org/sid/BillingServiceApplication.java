package org.sid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;

@Entity
@Data @AllArgsConstructor @NoArgsConstructor @ToString
class Bill{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date billingDate;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long customerId;
    @Transient
    private Customer customer;
    @OneToMany(mappedBy = "bill", fetch = FetchType.EAGER)
    private Collection<ProductItem> productItems;
}

@Entity
@Data @AllArgsConstructor @NoArgsConstructor
class ProductItem{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long productID;
    @Transient
    private Product product;
    private double price;
    private double quantity;
    @ManyToOne @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Bill bill;

    @Override
    public String toString() {
        return "ProductItem{" +
                "id=" + id +
                ", productID=" + productID +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}

@Data
class Customer{
    private Long id;
    private String name;
    private String email;
}

@FeignClient(name = "CUSTOMER-SERVICE")
interface CustomerService{
    @GetMapping("/customers/{id}")
    public Customer findCustomerById(@PathVariable(name = "id") Long id);
}

@Data
class Product{
    private Long id;
    private String name;
    private double price;
}

@FeignClient(name = "INVENTORY-SERVICE")
interface InventoryService{
    @GetMapping(value = "/products/{id}")
    public Product findProductById(@PathVariable(name = "id") Long id);
    @GetMapping(value = "/products")
    public PagedModel<Product> findAllProducts();
}

@Projection(name = "fullBill", types = Bill.class)
interface BillProjection{
    public Long getId();
    public Date getBillingDate();
    public Long getCustomerId();
    public Collection<ProductItem> getProductItems();
}

@RepositoryRestResource
interface BillRepository extends JpaRepository<Bill, Long>{}

@RepositoryRestResource
interface ProductItemRepositoy extends JpaRepository<ProductItem, Long>{}

@SpringBootApplication
@EnableFeignClients
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }


    @Bean
    CommandLineRunner start(BillRepository billRepository,
                            ProductItemRepositoy productItemRepositoy,
                            CustomerService customerService,
                            InventoryService inventoryService
    ){
        return args -> {
            Customer c1 = customerService.findCustomerById(1L);

            System.out.println("***************************");
            System.out.println("ID=" + c1.getId());
            System.out.println("Name=" + c1.getName());
            System.out.println("Email=" + c1.getEmail());
            System.out.println("***************************");

            Bill bill1 = billRepository.save(new Bill(null, new Date(), c1.getId(), null, null));

            PagedModel<Product> products = inventoryService.findAllProducts();
            products.getContent().forEach(product -> {
                productItemRepositoy.save(new ProductItem(null, product.getId(), null, product.getPrice(), 30, bill1));
            });

//            Product p1 = inventoryService.findProductById(1L);
//            productItemRepositoy.save(new ProductItem(null, p1.getId(), p1.getPrice(), 30, bill1));
//            System.out.println("***************************");
//            System.out.println("Product ID=" + p1.getId());
//            System.out.println("Product Name=" + p1.getName());
//            System.out.println("Product Price=" + p1.getPrice());
//            System.out.println("***************************");
//
//            Product p2 = inventoryService.findProductById(2L);
//            productItemRepositoy.save(new ProductItem(null, p2.getId(), p2.getPrice(), 15, bill1));
//
//            Product p3 = inventoryService.findProductById(3L);
//            productItemRepositoy.save(new ProductItem(null, p3.getId(), p3.getPrice(), 78, bill1));
//
//            productItemRepositoy.findAll().forEach(System.out::println);

            billRepository.findAll().forEach(System.out::println);
        };
    }
}

@RestController
class BillRestController{
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private ProductItemRepositoy productItemRepositoy;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/fullBill/{id}")
    public Bill getBill(@PathVariable(name = "id") Long id){
        Bill bill = billRepository.findById(id).get();
        bill.setCustomer(customerService.findCustomerById(bill.getCustomerId()));
        bill.getProductItems().forEach(productItem -> {
            productItem.setProduct(inventoryService.findProductById(productItem.getProductID()));
        });
        return bill;
    }
}
