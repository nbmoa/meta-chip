SUMMARY = "U-boot boot scripts for CHIP boards"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit deploy

DEPENDS = "u-boot-mkimage-native"

SRC_URI = "file://boot.cmd.in"

NAND_FLASH_START_ADDR = "0x00000000"
SPL_MEMIMG_ADDR = "0x44000000"
UBOOT_MEMIMG_ADDR = "0x4a000000"
SPL_FLASH_UPPER_ADDR = "0x400000"
LED_I2C_CHIP = "0x34"
LED_I2C_ADDR = "0x93"
UBOOT_FLASH_ADDR = "0x800000"
OOB_SIZE = "1664"
SCRIPTADDR = "0x43100000"

do_compile[depends] += "u-boot-chip:do_deploy"
do_compile() {
    PADDED_SPL_SIZE_BLOCKS=$(stat --dereference --printf="%s" "${DEPLOY_DIR_IMAGE}/${SPL_ECC_BINARY}")
    PADDED_SPL_SIZE_BLOCKS=$(expr $PADDED_SPL_SIZE_BLOCKS / \( ${CHIP_UBI_PAGE_SIZE} + ${OOB_SIZE} \))
    PADDED_SPL_SIZE_BLOCKS=$(echo $PADDED_SPL_SIZE_BLOCKS | xargs printf "0x%08x")
    PADDED_UBOOT_SIZE=$(stat --dereference  --printf="%s" "${DEPLOY_DIR_IMAGE}/${UBOOT_BINARY}" | xargs printf "0x%08x")

    sed -e "s,@NAND_FLASH_START_ADDR@,${NAND_FLASH_START_ADDR},g" \
        -e "s,@SPL_MEMIMG_ADDR@,${SPL_MEMIMG_ADDR},g" \
        -e "s,@UBOOT_MEMIMG_ADDR@,${UBOOT_MEMIMG_ADDR},g" \
        -e "s,@SPL_FLASH_UPPER_ADDR@,${SPL_FLASH_UPPER_ADDR},g" \
        -e "s,@LED_I2C_CHIP@,${LED_I2C_CHIP},g" \
        -e "s,@LED_I2C_ADDR@,${LED_I2C_ADDR},g" \
        -e "s,@PADDED_SPL_SIZE_BLOCKS@,${PADDED_SPL_SIZE_BLOCKS},g" \
        -e "s,@PADDED_UBOOT_SIZE@,${PADDED_UBOOT_SIZE},g" \
        -e "s,@UBOOT_FLASH_ADDR@,${UBOOT_FLASH_ADDR},g" \
        < "${WORKDIR}/boot.cmd.in" > "${WORKDIR}/boot.cmd"
    mkimage -A arm -T script -C none -n "Boot script" -d "${WORKDIR}/boot.cmd" "${WORKDIR}/boot.scr"
}

do_deploy() {
    install -d ${DEPLOYDIR}
    install -m 0644 ${WORKDIR}/boot.scr ${DEPLOYDIR}/boot.scr-${PV}-${PR}
    ln -sf boot.scr-${PV}-${PR} ${DEPLOYDIR}/boot.scr

    cat > ${DEPLOYDIR}/flash_CHIP_board.sh-${PV}-${PR} <<-EOF
	#!/bin/sh
	#
	
	if [ ! -n "\${UBI_IMAGE}" ]; then
	    echo "Error: UBI_IMAGE environment variable unset."
	    echo "Please set UBI_IMAGE to the basename of the root filesystem image to deploy"
	    exit -1
	elif [ ! -e "${DEPLOY_DIR_IMAGE}/\${UBI_IMAGE}" ]; then
	    echo "Error: UBI_IMAGE file \"${DEPLOY_DIR_IMAGE}/\${UBI_IMAGE}\" does not exist."
	    exit -1
	fi

	if ! type -path sunxi-fel > /dev/null 2>/dev/null; then
	    echo Unable to find sunxi-fel in PATH.
	    echo Please install the sunxi utilities for your desktop OS.
	    exit -1
	fi
	if ! type -path img2simg > /dev/null 2>/dev/null; then
	    echo Unable to find img2simg in PATH.
	    echo Please install the Android fastboot and related utilities for your desktop OS.
	    exit -1
	fi
	if ! type -path fastboot > /dev/null 2>/dev/null; then
	    echo Unable to find fastboot in PATH.
	    echo Please install the Android fastboot and related utilities for your desktop OS.
	    exit -1
	fi

	wait_for_fel() {
	    echo -n "waiting for fel..."
	    for i in {30..0}; do
	        if sunxi-fel ver 2>/dev/null >/dev/null; then
	            echo "OK"
	            return 0
	        fi
	        echo -n "."
	        sleep 1
	    done
	    echo "TIMEOUT"
	    return 1
	}
	FASTBOOT="fastboot -i 0x1f3a"
	wait_for_fastboot() {
	    echo -n "waiting for fastboot..."
	    for i in {30..0}; do
	        if [[ ! -z "\$(\${FASTBOOT} devices)" ]]; then
	            echo "OK"
	            return 0
	        fi
	        echo -n "."
	        sleep 1
	    done
	    echo "TIMEOUT"
	    return 1
	}
	if wait_for_fel; then
	    sunxi-fel spl ${DEPLOY_DIR_IMAGE}/${SPL_BINARY}
	    sunxi-fel --progress write ${SPL_MEMIMG_ADDR} ${DEPLOY_DIR_IMAGE}/${SPL_ECC_BINARY}
	    sunxi-fel --progress write ${UBOOT_MEMIMG_ADDR} ${DEPLOY_DIR_IMAGE}/${UBOOT_BINARY}
	    sunxi-fel --progress write ${SCRIPTADDR} ${DEPLOY_DIR_IMAGE}/boot.scr
	    sunxi-fel exe ${UBOOT_MEMIMG_ADDR}

	    if wait_for_fastboot; then
	        SIMG=\$(mktemp --suffix .ubi.sparse)
	        img2simg ${DEPLOY_DIR_IMAGE}/\${UBI_IMAGE} \${SIMG} ${CHIP_UBI_PAGE_SIZE}
	        \${FASTBOOT} flash UBI \${SIMG}
	        echo rm -f \${SIMG}
	        \${FASTBOOT} continue
	    fi
	fi
	EOF
    chmod +x ${DEPLOYDIR}/flash_CHIP_board.sh-${PV}-${PR}

    ln -sf flash_CHIP_board.sh-${PV}-${PR} ${DEPLOYDIR}/flash_CHIP_board.sh
}

addtask do_deploy after do_compile before do_build

COMPATIBLE_MACHINE = "chip"
