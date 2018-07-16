SUMMARY = "BS realtek wifi"

LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://core/rtw_ap.c;beginline=3;endline=12;md5=488c90996933f63c447ef3420cc03079"

inherit module

SRCREV = "ae46fc7e9b6ff65a194cee7c6f50398e5f99fdd4"
SRC_URI = "git://github.com/NextThingCo/RTL8723BS;protocol=https;branch=master \
           file://0001-rtl8723bs-add-modules_install-and-correct-depmod.patch \
           file://0001-rtl8723bs-Disable-CONFIG_DEBUG.patch \
           file://0001-rtl8723bs-Remove-debug-prints.patch \
          "

S = "${WORKDIR}/git"

EXTRA_OEMAKE = "KSRC=${STAGING_KERNEL_DIR} \
                KVER=${KERNEL_VERSION} \
                SUBARCH=${ARCH} \
                ARCH=${ARCH} \
                MODDESTDIR=${D}/lib/modules/${KERNEL_VERSION}/kernel/drivers/net/wireless/ \
               "
PKGV = "${KERNEL_VERSION}"

