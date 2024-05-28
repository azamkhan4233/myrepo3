////package com.calmaapp.entity;
////
////import com.fasterxml.jackson.annotation.JsonIgnore;
////
////import jakarta.persistence.*;
////import lombok.Getter;
////import lombok.NoArgsConstructor;
////import lombok.Setter;
////
////@Entity
////@Getter
////@Setter
////@NoArgsConstructor
////@Table(name = "service")
////public class ServicesProvided {
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    private Long id;
////
////    private String serviceName;
////    private double cost;
////
////    @ManyToOne(fetch = FetchType.EAGER)
////    @JoinColumn(name = "salon_id")
////    @JsonIgnore
////    private Salon salon;
////}
//package com.calmaapp.entity;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import jakarta.persistence.*;
//
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Getter
//@Setter
//@NoArgsConstructor
//@Table(name = "service")
//public class ServicesProvided {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String serviceName;
//    private double cost;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "salon_id")
//    @JsonBackReference
//    private Salon salon;
//}
package com.calmaapp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "service")
public class ServicesProvided {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private double cost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    @JsonBackReference
    private Salon salon;
}
