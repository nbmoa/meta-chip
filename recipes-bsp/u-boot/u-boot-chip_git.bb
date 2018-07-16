require recipes-bsp/u-boot/u-boot.inc

DESCRIPTION = "U-Boot port for C.H.I.P. boards"
LICENSE = "GPLv2+"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=0507cd7da8e7ad6d6701926ec9b84c95"

DEPENDS += "dtc-native"
PROVIDES += "u-boot"
RDEPENDS_${PN}_append_chip = " chip-u-boot-scr"

UBOOT_VERSION ?= "2016.01"
PV = "${UBOOT_VERSION}+git${SRCPV}"

SRCREV ?= "99c771f6ee154ba7c0c8c45611affd17609a97b0"
BRANCH ?= "production-mlc"
SRC_URI = " \
	git://github.com/NextThingCo/CHIP-u-boot.git;branch=${BRANCH} \
	file://gcc6.patch \
	file://0001-Add-bootcmd_ubi-support-for-sunxi-boards.patch \
	"
S = "${WORKDIR}/git"

do_compile_append() {
    install ${B}/spl/${SPL_ECC_BINARY} ${B}/${SPL_ECC_BINARY}
    install ${B}/spl/${SPL_BINARY} ${B}/${SPL_BINARY}
}

COMPATIBLE_MACHINE = "chip"

do_deploy_append() {
    install -m 644 ${B}/${SPL_ECC_BINARY} ${DEPLOYDIR}/${SPL_ECC_BINARY}-${PV}-${PR}
    ln -sf ${SPL_ECC_BINARY}-${PV}-${PR} ${DEPLOYDIR}/${SPL_ECC_BINARY}
}
